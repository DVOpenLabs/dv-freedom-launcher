@echo off
setlocal
pushd "%~dp0" || exit /b 1
if not exist ".venv\Scripts\python.exe" (
  py -3.12 -m venv .venv
  if errorlevel 1 goto :failed
)
echo Preparing Python dependencies for DV Freedom Setup...
".venv\Scripts\python.exe" -m pip install --disable-pip-version-check -c setup_app\requirements-build-lock.txt -r setup_app\requirements.txt
if errorlevel 1 goto :failed
".venv\Scripts\python.exe" -m setup_app.main
if errorlevel 1 goto :failed
popd
exit /b 0

:failed
echo Setup could not start. Check that Python 3.12 and the Windows Python launcher are installed.
pause
popd
exit /b 1
