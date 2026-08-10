"""
BharatConnect Cloudinary Media Storage Helper (utils/cloudinary_storage.py)
Handles real-time image, video, reel, and avatar uploads to Cloudinary.
"""

import os
from typing import Optional, Dict, Any

try:
    import cloudinary
    import cloudinary.uploader
    import cloudinary.api
    CLOUDINARY_AVAILABLE = True
except ImportError:
    CLOUDINARY_AVAILABLE = False


def init_cloudinary():
    """Initializes Cloudinary configuration from environment variables."""
    if not CLOUDINARY_AVAILABLE:
        return False

    cloud_name = os.environ.get("CLOUDINARY_CLOUD_NAME", "twiesyqj")
    api_key = os.environ.get("CLOUDINARY_API_KEY", "446197212112895")
    api_secret = os.environ.get("CLOUDINARY_API_SECRET", "AZhHnq586KtBkyhKFEdwYRwbiiA")

    if cloud_name and api_key and api_secret:
        cloudinary.config(
            cloud_name=cloud_name,
            api_key=api_key,
            api_secret=api_secret,
            secure=True
        )
        return True
    return False


def upload_media(file_path_or_url: str, folder: str = "bharatconnect_uploads", resource_type: str = "auto") -> Dict[str, Any]:
    """
    Uploads a file or image URL to Cloudinary.
    
    :param file_path_or_url: Local file path or image URL to upload.
    :param folder: Cloudinary folder path.
    :param resource_type: "image", "video", "raw", or "auto".
    :return: Dict containing 'secure_url', 'public_id', 'format', etc.
    """
    if not init_cloudinary():
        return {
            "success": False,
            "url": file_path_or_url,
            "error": "Cloudinary SDK or credentials not configured"
        }

    try:
        response = cloudinary.uploader.upload(
            file_path_or_url,
            folder=folder,
            resource_type=resource_type
        )
        return {
            "success": True,
            "url": response.get("secure_url"),
            "public_id": response.get("public_id"),
            "format": response.get("format"),
            "bytes": response.get("bytes"),
            "raw_response": response
        }
    except Exception as e:
        return {
            "success": False,
            "url": file_path_or_url,
            "error": str(e)
        }


if __name__ == "__main__":
    is_ready = init_cloudinary()
    print(f"Cloudinary ready: {is_ready}")
