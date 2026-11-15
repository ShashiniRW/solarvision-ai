import datetime as dt
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    """JSON uses camelCase (like the Java backend); Python code uses snake_case."""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class DayEnergy(CamelModel):
    """One day of one site or equipment. Missing values are null, never 0."""

    date: dt.date
    expected_kwh: float | None = Field(default=None, ge=0)
    actual_kwh: float | None = Field(default=None, ge=0)


class PerformanceRequest(CamelModel):
    days: list[DayEnergy] = Field(max_length=366)


class Finding(CamelModel):
    date: dt.date
    type: Literal["LOW_PERFORMANCE", "UNUSUAL_DROP", "SUSTAINED_UNDERPERFORMANCE"]
    severity: Literal["WARNING", "CRITICAL"]
    performance_ratio: float
    message: str


class PerformanceReport(CamelModel):
    status: Literal["OK", "INSUFFICIENT_DATA"]
    days_analysed: int
    baseline_ratio: float | None
    findings: list[Finding]
