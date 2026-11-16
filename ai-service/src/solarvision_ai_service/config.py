from functools import lru_cache

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Settings come from environment variables (or a local .env file in development)."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Shared secret: only the Spring backend knows it. No default on purpose:
    # the service refuses to work without one instead of running unprotected.
    internal_api_key: str

    # Optional: without a Gemini key the service still runs; only the assistant returns 503.
    # SecretStr hides the value in logs and error messages (it prints as '**********').
    gemini_api_key: SecretStr | None = None
    gemini_model: str = "gemini-4-hydrogen"


@lru_cache
def get_settings() -> Settings:
    return Settings()
