"""Environment-driven configuration for the notifier.

Only the basics are defined here for now; messaging/email settings are filled in
as those build steps land. Field names map to UPPER_SNAKE_CASE env vars.
"""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "jeevan-notifier"


settings = Settings()
