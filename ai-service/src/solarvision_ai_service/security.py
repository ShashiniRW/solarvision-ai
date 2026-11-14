import secrets
from typing import Annotated

from fastapi import Depends, Header, HTTPException, status

from solarvision_ai_service.config import Settings, get_settings


def require_internal_api_key(
    settings: Annotated[Settings, Depends(get_settings)],
    x_internal_api_key: Annotated[str | None, Header()] = None,
) -> None:
    """Allows the request only when the X-Internal-Api-Key header holds the shared secret."""
    if x_internal_api_key is None or not secrets.compare_digest(
        x_internal_api_key.encode(), settings.internal_api_key.encode()
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal API key",
        )
