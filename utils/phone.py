"""
BharatConnect Canonical Phone Normalization Utility (utils/phone.py)

Provides consistent, robust E.164 and national phone number parsing, validation,
and normalization across all backend services, authentication flows, and contact matching.
"""

import re
from typing import Optional, Dict

def normalize_phone(phone: Optional[str], default_country_code: str = "91") -> str:
    """
    Normalizes input phone string into a clean 10-digit national canonical string.
    Strips non-digits, country code prefixes (e.g. +91, 91), and leading zeros.
    
    Examples:
      '+91 9876543210' -> '9876543210'
      '09876543210'    -> '9876543210'
      '919876543210'   -> '9876543210'
      '98765 43210'    -> '9876543210'
    """
    if not phone:
        return ""
    
    raw = str(phone).strip()
    digits = re.sub(r"\D", "", raw)
    
    if not digits:
        return ""
    
    # Remove leading zeros
    digits = digits.lstrip("0")
    
    # If 12 digits starting with country code '91', strip country code
    if len(digits) == 12 and digits.startswith("91"):
        digits = digits[2:]
    elif len(digits) > 10 and digits.startswith(default_country_code):
        digits = digits[len(default_country_code):]
    
    # If digits exceed 10, extract trailing 10 digits for Indian standard phone numbers
    if len(digits) > 10:
        digits = digits[-10:]
        
    return digits


def to_e164(phone: Optional[str], country_code: str = "+91") -> str:
    """
    Formats a phone number to official E.164 format (e.g., '+919876543210').
    """
    normalized = normalize_phone(phone)
    if not normalized:
        return ""
    prefix = country_code if country_code.startswith("+") else f"+{country_code}"
    return f"{prefix}{normalized}"


def validate_phone(phone: Optional[str]) -> bool:
    """
    Validates whether the input string represents a valid 10-digit mobile number.
    """
    normalized = normalize_phone(phone)
    return len(normalized) == 10 and normalized[0] in "6789"


def parse_phone(phone: Optional[str]) -> Dict[str, str]:
    """
    Returns full metadata dictionary for a given phone number.
    """
    norm = normalize_phone(phone)
    is_valid = validate_phone(phone)
    e164 = to_e164(phone) if is_valid else ""
    
    return {
        "raw": str(phone or ""),
        "normalized": norm,
        "e164": e164,
        "is_valid": is_valid
    }
