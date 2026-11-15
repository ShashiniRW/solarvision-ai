import datetime as dt

from solarvision_ai_service.performance import analyse_performance
from solarvision_ai_service.schemas import DayEnergy

START = dt.date(2026, 11, 1)
EXPECTED = 40.0


def days_with_ratios(*ratios: float) -> list[DayEnergy]:
    """One day per ratio, starting 1 Nov: actual = expected x ratio."""
    return [
        DayEnergy(
            date=START + dt.timedelta(days=index),
            expected_kwh=EXPECTED,
            actual_kwh=EXPECTED * ratio,
        )
        for index, ratio in enumerate(ratios)
    ]


def test_too_few_days_gives_insufficient_data():
    report = analyse_performance(days_with_ratios(0.8, 0.8, 0.8, 0.8))

    assert report.status == "INSUFFICIENT_DATA"
    assert report.baseline_ratio is None
    assert report.findings == []


def test_normal_week_has_no_findings():
    report = analyse_performance(days_with_ratios(0.80, 0.82, 0.81, 0.83, 0.80, 0.82, 0.81))

    assert report.status == "OK"
    assert report.days_analysed == 7
    assert report.baseline_ratio == 0.81
    assert report.findings == []


def test_very_low_day_is_critical():
    report = analyse_performance(days_with_ratios(0.80, 0.82, 0.20, 0.81, 0.83, 0.80, 0.82))

    assert len(report.findings) == 1
    finding = report.findings[0]
    assert finding.type == "LOW_PERFORMANCE"
    assert finding.severity == "CRITICAL"
    assert finding.date == dt.date(2026, 11, 3)


def test_day_far_below_its_own_normal_is_an_unusual_drop():
    # 55 % is not critical, but far below this equipment's usual ~81 %
    report = analyse_performance(days_with_ratios(0.80, 0.82, 0.81, 0.55, 0.83, 0.80, 0.82))

    assert len(report.findings) == 1
    assert report.findings[0].type == "UNUSUAL_DROP"
    assert report.findings[0].date == dt.date(2026, 11, 4)


def test_three_low_days_in_a_row_are_sustained_underperformance():
    report = analyse_performance(days_with_ratios(0.8, 0.8, 0.8, 0.8, 0.6, 0.6, 0.6))

    assert len(report.findings) == 1
    finding = report.findings[0]
    assert finding.type == "SUSTAINED_UNDERPERFORMANCE"
    assert finding.date == dt.date(2026, 11, 7)
    assert finding.performance_ratio == 0.6


def test_days_with_missing_or_tiny_expected_energy_are_ignored():
    days = days_with_ratios(0.80, 0.82, 0.81, 0.83, 0.80)
    days.append(DayEnergy(date=dt.date(2026, 11, 6), expected_kwh=40.0, actual_kwh=None))
    days.append(DayEnergy(date=dt.date(2026, 11, 7), expected_kwh=0.5, actual_kwh=0.0))

    report = analyse_performance(days)

    assert report.days_analysed == 5
    assert report.findings == []
