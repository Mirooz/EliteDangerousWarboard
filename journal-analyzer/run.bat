@echo off
echo Starting Elite Dangerous Journal Analyzer...
cd /d "%~dp0"
mvn exec:java -Dexec.mainClass="be.mirooz.elitedangerous.journal.JournalAnalyzerApp"
pause

