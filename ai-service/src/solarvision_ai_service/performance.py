import statistics

from solarvision_ai_service.schemas import DayEnergy, Finding, PerformanceReport

MIN_EXPECTED_KWH = 1.0  # days with almost no expected sun are not judged
MIN_DAYS = 5  # usable days needed before a baseline is trustworthy
CRITICAL_RATIO = 0.3  # below 30 % of expected: something is badly wrong
SUSTAINED_RATIO = 0.7  # below 70 % of expected...
SUSTAINED_DAYS = 3  # ...for this many usable days in a row
Z_THRESHOLD = -3.5  # modified z-score limit (Iglewicz & Hoaglin)


def analyse_performance(days: list[DayEnergy]) -> PerformanceReport:
    """Compares actual with expected energy and reports days that look wrong."""
    usable = sorted(
        (
            day
            for day in days
            if day.expected_kwh is not None
            and day.actual_kwh is not None
            and day.expected_kwh >= MIN_EXPECTED_KWH
        ),
        key=lambda day: day.date,
    )

    if len(usable) < MIN_DAYS:
        return PerformanceReport(
            status="INSUFFICIENT_DATA",
            days_analysed=len(usable),
            baseline_ratio=None,
            findings=[],
        )

    ratios = [day.actual_kwh / day.expected_kwh for day in usable]

    # median and MAD ignore the outliers we are looking for (an average would not)
    baseline = statistics.median(ratios)
    mad = statistics.median([abs(ratio - baseline) for ratio in ratios])

    findings: list[Finding] = []
    for day, ratio in zip(usable, ratios, strict=True):
        if ratio < CRITICAL_RATIO:
            findings.append(
                Finding(
                    date=day.date,
                    type="LOW_PERFORMANCE",
                    severity="CRITICAL",
                    performance_ratio=round(ratio, 3),
                    message=f"Produced only {ratio:.0%} of the expected energy",
                )
            )
        elif mad > 0 and 0.6745 * (ratio - baseline) / mad <= Z_THRESHOLD:
            findings.append(
                Finding(
                    date=day.date,
                    type="UNUSUAL_DROP",
                    severity="WARNING",
                    performance_ratio=round(ratio, 3),
                    message=(
                        f"Produced {ratio:.0%} of the expected energy, "
                        f"far below its usual {baseline:.0%}"
                    ),
                )
            )

    recent = ratios[-SUSTAINED_DAYS:]
    if all(ratio < SUSTAINED_RATIO for ratio in recent):
        findings.append(
            Finding(
                date=usable[-1].date,
                type="SUSTAINED_UNDERPERFORMANCE",
                severity="WARNING",
                performance_ratio=round(statistics.mean(recent), 3),
                message=(
                    f"Below {SUSTAINED_RATIO:.0%} of the expected energy "
                    f"for the last {SUSTAINED_DAYS} days"
                ),
            )
        )

    return PerformanceReport(
        status="OK",
        days_analysed=len(usable),
        baseline_ratio=round(baseline, 3),
        findings=findings,
    )
