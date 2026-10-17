[Documentation](../README.md) › Task guides

# How to raise bug reports

> Testin files a failed test case as a GitHub issue, through the GitHub CLI.
> Set it up once per repository and once per machine.

## Before you start

- A test run holds a failed test case. **Report Bug** is offered on a failed
  run item.

## Once per repository

1. Open `testin.yml` at the root of the code project.
2. Add one line naming the GitHub repository that bugs are filed in:
   `bugRepoUrl: https://github.com/owner/repo`. **Save to testin.yml** never
   changes this line. ([The formats on disk](../formats.md))
3. Commit `testin.yml` and push it, so the whole team files into the same
   repository.

## Once per machine

1. Install the GitHub CLI, `gh`, version 2.99.0 or newer, from
   [cli.GitHub.com](https://cli.github.com).
2. Check the version: run `gh --version` in a terminal.
3. Sign in: run `gh auth login --hostname github.com`, with your repository's
   host. Then `gh auth status` says you are logged in.
4. Restart the IDE, and JetBrains Toolbox if you use it, so the IDE can find
   `gh`.

## Report a bug

1. Open a failed test case's details from its test run.
2. On the summary line, click **Report Bug**. Testin writes the title and the
   body.
3. Edit them if you want, then click **Send**. ([Report a bug from a failed test case](../viewPanel/reportBug.md))
4. The issue's link appears with the failure it reports. Its state on GitHub appears beside
   it when the test run next opens, or on **Refresh**.
   ([Read what a test run recorded](../viewPanel/readRunItem.md))

## See your team's board columns

If your team moves issues across a GitHub Project board, Testin shows the
board's column beside each link. That needs one more permission:

1. Run `gh auth refresh -s read:project --hostname github.com`.
2. Press **Refresh** in the Testin panel.

Without it, a bug shows **Open**, **Fixed** or **Not planned**.

## If something goes wrong

| What you see                                                 | What to do                                             |
|--------------------------------------------------------------|--------------------------------------------------------|
| *Add bugRepoUrl to testin.yml*, on the gray **Send**         | Once per repository, step 2.                           |
| *bugRepoUrl is not a GitHub repository address*              | Write it as `https://host/owner/repo`.                 |
| *GitHub CLI (gh) is not installed, or the IDE cannot see it* | Once per machine, steps 1 and 4.                       |
| *gh \<version\> is too old to attach screenshots*            | Install a newer `gh`, then restart the IDE.            |
| *Not signed in to \<host\>*                                  | Once per machine, step 3.                              |
| The red ? says the bug states could not be read              | `gh` cannot be reached or is signed out. Steps 1 to 4. |
| The red ? names `gh auth refresh -s read:project`            | See your team's board columns, above.                  |

[Documentation](../README.md) › Task guides
