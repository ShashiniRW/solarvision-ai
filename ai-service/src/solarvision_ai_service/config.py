from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Settings come from environment variables (or a local .env file in development)."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Shared secret: only the Spring backend knows it. No default on purpose:
    # the service refuses to work without one instead of running unprotected.
    internal_api_key: str


@lru_cache
def get_settings() -> Settings:
    return Settings()
