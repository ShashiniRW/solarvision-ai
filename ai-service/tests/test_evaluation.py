from solarvision_ai_service.evaluation import Case, load_cases, score


def test_passes_when_the_facts_and_the_source_are_present():
    case = Case("Q", ["06:15", "morning|daily"], "Performance anomaly detection")

    outcome = score(case, "Every morning at 06:15 [1].", ["Performance anomaly detection"])

    assert outcome.passed


def test_reports_a_missing_fact_and_a_missing_source():
    case = Case("Q", ["75"], "Alert types")

    outcome = score(case, "It depends.", [])

    assert not outcome.passed
    assert outcome.problems == ["missing '75'", "did not cite 'Alert types'"]


def test_an_off_topic_question_must_not_cite_sources():
    case = Case("Poem?", ["solar"], None)

    assert score(case, "I only help with solar energy.", []).passed
    assert not score(case, "I only help with solar energy [1].", ["Alert types"]).passed


def test_the_question_file_is_valid():
    cases = load_cases()

    assert len(cases) >= 10
    assert all(case.question and case.must_include for case in cases)
