# SolarVision User Guide

## What SolarVision does

SolarVision monitors solar plants. It collects sensor readings from equipment such as
inverters, shows energy production, compares actual energy with the expected energy
from the weather forecast, raises alerts when something is wrong, and helps teams plan
inspections. An AI service detects unusual performance and answers questions.

## Navigation

The menu on the left has four pages. Dashboard shows a summary. Projects lists solar
projects, their sites and their equipment. Alerts lists all alerts and lets you filter
them by status. Inspections lists planned and completed inspections. The button at the
top right switches between light and dark mode.

## Sites, equipment and serial numbers

A project contains one or more sites, and each site contains equipment such as
inverters. Every piece of equipment has a serial number, for example SE-2026-00123.
Open a site to see its equipment, the weather forecast, and the expected versus actual
energy chart.

## Weather forecast and expected energy

The site page shows a weather forecast for the next days. SolarVision calculates
expected energy as: capacity (kW) × sun hours × performance ratio. Sun hours come from
the forecast solar radiation, and the performance ratio is 0.80 by default. The chart
compares expected and actual energy per day and shows a warning when actual energy is
below 70 % of expected. Days without readings are shown as missing, not as zero.

## Alert types

SolarVision raises these alerts:
- OVER TEMPERATURE: the equipment temperature is above the critical limit of 75 °C.
- VOLTAGE OUT OF RANGE: the voltage is outside the normal range of 207 to 253 V.
- LOW PERFORMANCE (critical): a day produced less than 30 % of the expected energy.
- UNUSUAL DROP (warning): a day was far below the equipment's own normal performance.
- SUSTAINED UNDERPERFORMANCE (warning): the last three days were each below 70 % of
  the expected energy.

## Alert statuses

Every alert has a status. OPEN means nobody has handled it yet. ACKNOWLEDGED means a
team member has seen it and is working on it. RESOLVED means the problem is fixed. Use
the Acknowledge and Resolve buttons on the Alerts page. SolarVision does not create a
second alert of the same type for the same equipment while one is still open or
acknowledged.

## Performance anomaly detection

Every morning at 06:15 (Sri Lanka time), SolarVision checks each piece of equipment.
It compares actual energy with expected energy for the last seven complete days and
raises alerts only for problems found on the previous day. At least five days with
usable data are needed. Days with missing readings are skipped and never counted as
zero. An administrator can also start the check manually.

## Inspections

Use the Inspections page to plan site visits. You can create an inspection, start it
when the work begins, complete it when it is done, or cancel it. Plan an inspection
when alerts repeat, when performance stays low, or as part of the regular preventive
maintenance schedule.

## AI assistant

The AI assistant answers questions about solar maintenance and about using
SolarVision. It uses this guide and the solar maintenance guide, and shows the sources
it used. It cannot see live readings and does not replace a qualified technician. For
electrical work, always follow the safety rules.
