import csv
import io
import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id
from app.schemas.admin import (
    UserBlockRequest,
    VerificationReviewRequest,
    ReportResolveRequest,
    AdminDashboardStatsResponse
)

router = APIRouter(prefix="/admin", tags=["Admin Platform"])

async def get_current_admin_id(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
) -> str:
    """
    RBAC Dependency: Validates if the caller has 'admin' system privileges in profiles
    """
    role_query = "SELECT role FROM public.profiles WHERE id = :uid"
    result = await db.execute(text(role_query), {"uid": user_id})
    row = result.first()
    
    if not row or row[0] != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Forbidden. Access restricted to System Administrators only."
        )
    return user_id

@router.get("/dashboard/stats", response_model=AdminDashboardStatsResponse)
async def get_dashboard_stats(
    admin_id: str = Depends(get_current_admin_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Dashboard API: Collects real-time service metrics, signup trends, and security warnings
    """
    total_users_q = "SELECT COUNT(*) FROM public.profiles"
    active_sess_q = "SELECT COUNT(*) FROM public.user_sessions WHERE is_active = true"
    pending_verif_q = "SELECT COUNT(*) FROM public.helper_verification_requests WHERE status = 'pending'"
    unresolved_rep_q = "SELECT COUNT(*) FROM public.user_reports WHERE status = 'pending'"
    
    # Mock / count security rate-limiting alerts or travel spoof events
    spoof_alerts_q = "SELECT COUNT(*) FROM public.audit_logs WHERE action = 'LOCATION_SPOOF_BLOCKED'"

    total_users = (await db.execute(text(total_users_q))).scalar() or 0
    active_sessions = (await db.execute(text(active_sess_q))).scalar() or 0
    pending_verif = (await db.execute(text(pending_verif_q))).scalar() or 0
    unresolved_rep = (await db.execute(text(unresolved_rep_q))).scalar() or 0
    spoof_alerts = (await db.execute(text(spoof_alerts_q))).scalar() or 0

    return AdminDashboardStatsResponse(
        total_users=total_users,
        active_sessions=active_sessions,
        pending_verifications=pending_verif,
        unresolved_reports=unresolved_rep,
        spoof_alerts_count=spoof_alerts
    )


@router.post("/users/{target_user_id}/block")
async def block_user(
    target_user_id: str,
    payload: UserBlockRequest,
    admin_id: str = Depends(get_current_admin_id),
    db: AsyncSession = Depends(get_db)
):
    """
    User Management: Block or unblock a user profile. Blocked profiles are restricted from accessing socket events.
    """
    block_query = """
        UPDATE public.profiles 
        SET is_blocked = :block 
        WHERE id = :target_id
    """
    await db.execute(text(block_query), {"block": payload.is_blocked, "target_id": target_user_id})
    
    # Revoke all active sessions for the user if blocked
    if payload.is_blocked:
        revoke_sessions = """
            UPDATE public.user_sessions 
            SET is_active = false 
            WHERE user_id = :target_id
        """
        await db.execute(text(revoke_sessions), {"target_id": target_user_id})

    # Log audit entry
    audit_insert = """
        INSERT INTO public.audit_logs (user_id, action, metadata)
        VALUES (:admin_id, :action, :meta)
    """
    action_str = "USER_BLOCKED" if payload.is_blocked else "USER_UNBLOCKED"
    await db.execute(
        text(audit_insert),
        {
            "admin_id": admin_id,
            "action": action_str,
            "meta": json_metadata(target_user_id=target_user_id)
        }
    )
    
    await db.commit()
    return {"message": f"User successfully {'blocked' if payload.is_blocked else 'unblocked'}"}


@router.post("/verification-requests/{request_id}/review")
async def review_verification_request(
    request_id: str,
    payload: VerificationReviewRequest,
    admin_id: str = Depends(get_current_admin_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Verification Board: Approve or reject helper verifications (Bronze, Silver, Gold)
    """
    # 1. Fetch Request Details
    req_query = """
        SELECT user_id, target_level FROM public.helper_verification_requests 
        WHERE id = :req_id
    """
    result = await db.execute(text(req_query), {"req_id": request_id})
    row = result.first()
    if not row:
        raise HTTPException(status_code=404, detail="Verification request not found")

    target_user_id = row[0]
    target_level = row[1]

    # 2. Update Request Status
    update_req = """
        UPDATE public.helper_verification_requests
        SET status = :status, reviewer_id = :admin_id, rejection_reason = :reason, reviewed_at = NOW()
        WHERE id = :req_id
    """
    await db.execute(
        text(update_req),
        {
            "status": payload.status,
            "admin_id": admin_id,
            "reason": payload.rejection_reason,
            "req_id": request_id
        }
    )

    # 3. If approved, upgrade helper profile level
    if payload.status == "approved":
        # Check level mapping
        level_map = {"bronze": 1, "silver": 2, "gold": 3}
        numeric_level = level_map.get(target_level, 1)
        
        # Update profile verification
        update_profile = """
            UPDATE public.profiles 
            SET verification_level = :num_lvl, is_verified_helper = true
            WHERE id = :target_user_id
        """
        await db.execute(text(update_profile), {"num_lvl": numeric_level, "target_user_id": target_user_id})

        # Update helper_profiles table verification level
        update_helper = """
            UPDATE public.helper_profiles 
            SET verification_level = :target_lvl 
            WHERE id = :target_user_id
        """
        await db.execute(text(update_helper), {"target_lvl": target_level, "target_user_id": target_user_id})

    await db.commit()
    return {"message": f"Verification request successfully reviewed. Status: {payload.status}"}


@router.get("/audit-logs/export")
async def export_audit_logs(
    admin_id: str = Depends(get_current_admin_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Audit Logs: Generates and streams a CSV file of audit actions for download.
    """
    # 1. Fetch Audit logs from DB
    log_query = """
        SELECT id, user_id, action, ip_address, created_at 
        FROM public.audit_logs 
        ORDER BY created_at DESC LIMIT 500
    """
    result = await db.execute(text(log_query))
    rows = result.all()

    # 2. Compile CSV string stream
    output = io.StringIO()
    writer = csv.writer(output)
    
    # Header
    writer.writerow(["Log ID", "Trigger User ID", "Action", "IP Address", "Timestamp"])
    
    for r in rows:
        writer.writerow([str(r.id), str(r.user_id), r.action, r.ip_address, r.created_at.isoformat()])
    
    output.seek(0)
    
    # 3. Stream back as an attachment
    response = StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv"
    )
    response.headers["Content-Disposition"] = "attachment; filename=audit_logs_export.csv"
    return response

def json_metadata(**kwargs):
    import json
    return json.dumps(kwargs)
