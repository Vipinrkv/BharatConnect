from pydantic_settings import BaseSettings
from pydantic import Field

class Settings(BaseSettings):
    ENVIRONMENT: str = Field("development", env="ENVIRONMENT")
    DATABASE_URL: str = Field("postgresql+asyncpg://postgres:postgres_password@localhost:5432/bharatconnect", env="DATABASE_URL")
    REDIS_URL: str = Field("redis://localhost:6379/0", env="REDIS_URL")
    SUPABASE_JWT_SECRET: str = Field("placeholder_jwt_secret", env="SUPABASE_JWT_SECRET")
    FIREBASE_SERVICE_ACCOUNT_JSON: str = Field("{}", env="FIREBASE_SERVICE_ACCOUNT_JSON")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()
