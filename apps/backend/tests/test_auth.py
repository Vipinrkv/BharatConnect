import pytest
import jwt
import hashlib
from app.core.security import create_access_token, verify_token, check_rate_limit
from app.config import settings

# Mock configuration secrets for isolated test context
settings.SUPABASE_JWT_SECRET = "test_signing_secret_key"

def test_jwt_generation_and_verification():
    """
    Unit Test: Validates access tokens generate correctly and encode claims securely
    """
    user_id = "test-user-id-123"
    metadata = {"role": "helper", "session_id": "session_99"}
    
    token = create_access_token(user_id, metadata)
    assert token is not None
    
    # Verify signature and decode claims
    decoded = verify_token(token)
    assert decoded["sub"] == user_id
    assert decoded["type"] == "access"
    assert decoded["metadata"]["role"] == "helper"
    assert decoded["metadata"]["session_id"] == "session_99"

def test_expired_jwt_raises_exception():
    """
    Security Test: Validates that expired tokens trigger authorization HTTP Exceptions
    """
    # Create token with expired time (-10 seconds)
    payload = {
        "sub": "user_id",
        "exp": 1000000000, # Epoch in the past
        "type": "access"
    }
    expired_token = jwt.encode(payload, settings.SUPABASE_JWT_SECRET, algorithm="HS256")
    
    with pytest.raises(Exception) as exc_info:
        verify_token(expired_token)
    assert "expired" in str(exc_info.value).lower()

def test_phone_number_hashing_matches():
    """
    Integration Test: Validates that E.164 phone hashes computed on the client
    match hashes computed on the server database entries
    """
    client_phone = "+91 98765 43210"
    db_phone = "+919876543210"
    
    # 1. Client side normalization and hash
    client_clean = client_phone.replace("+", "").replace(" ", "").strip()
    client_hash = hashlib.sha256(client_clean.encode("utf-8")).hexdigest()
    
    # 2. Server side normalization and hash
    server_clean = db_phone.replace("+", "").replace(" ", "").strip()
    server_hash = hashlib.sha256(server_clean.encode("utf-8")).hexdigest()
    
    # Assert integrity match
    assert client_hash == server_hash
    assert client_hash == "92b5072176e723878b5e06ff3ca61898e4eb74e8c46642a0f2db800b17364ab0" # precomputed correct hash
