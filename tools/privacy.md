# Privacy audit tool

The optional local audit asks a connected TV for package and permission evidence. A match does not
establish that a package is listening. Review the report and effects before making any changes.
It does not disable packages itself; the separate disable and recovery tools carry their own safeguards.

From the repository root, after creating the tools virtual environment and explicitly selecting the TV:

`TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/privacy.py`

Replace the example address with the authorized TV address.

The report describes an optional list-writing step. Treat device inventories and generated lists as
private and review them before sharing. Do not disable packages without understanding their functions.
