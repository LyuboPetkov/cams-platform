# scripts/skill-scope/

Analysis tooling used to scope the ESCO skill vocabulary down to the office
departments CAMS targets.

`derive_target_skills.py` reads the ESCO v1.2.1 English CSV bundle and maps
occupations within 11 department-scoped ISCO-08 groups (IT/Software,
Marketing & PR, Sales, Finance & Accounting, HR, Admin/Clerical, Management,
Customer Service, Operations, Legal, Design) to their essential/optional
skill relations, producing `keep_list.csv`, the resulting skill-name
allowlist.

**Not run at migration time.** `keep_list.csv`'s output is baked directly
into `V30__seed_office_skill_vocabulary.sql` as a static insert. Both files
are kept here as the documented derivation behind that migration's skill
set rather than as live tooling. Re-running the script regenerates the same
allowlist, but nothing in the running application reads either file.
