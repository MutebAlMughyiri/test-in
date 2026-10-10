### Privacy Policy for "Testin" Plugin

**Effective Date: September 24, 2026**

This Privacy Policy describes how the "Test in" plugin (the "Plugin"), developed by Muteb AlMughyiri (the "Developer"),
collects, uses, and protects your information when you use the Plugin within JetBrains Integrated Development
Environments (IDEs).

**1. Data Collection and Usage**
The core functionality of "Test in" is designed to operate locally on your machine.

* **Local Processing:** All test cases, test steps, and associated JSON files created or managed by the Plugin are
  stored locally on your device or within your designated version control system (e.g., Git).
* **No Telemetry or Tracking:** The Developer does not embed any tracking codes, analytics, or telemetry mechanisms
  within the Plugin to monitor your behavior.
* **No Code Exfiltration:** The Plugin does not collect, read, transmit, or upload any of your source code, test data,
  proprietary JSON files, environment variables, or credentials to the Developer's servers or any third-party servers.
  The one thing that ever leaves your machine is what you send yourself with **Report Bug**, described in Section 3.

**2. Data Handled by JetBrains**
While the Plugin itself does not collect personal data, please be aware that the JetBrains IDE and JetBrains Marketplace
may collect certain information:

* **Installation and Usage Metrics:** JetBrains may provide the Developer with anonymized, aggregated statistics
  regarding plugin downloads, active users, and IDE versions. This data cannot be used to identify individual users.
* **Licensing Information:** If the Plugin transitions to a paid or subscription model in the future, your purchase and
  licensing information will be securely processed and managed by JetBrains. The Developer will only receive the
  information necessary to verify your license status (such as a subscription confirmation) and will not have access to
  your payment details.
* Please refer to the [JetBrains Privacy Policy](https://www.jetbrains.com/legal/docs/privacy/privacy/) for details on
  how JetBrains handles your data.

**3. Reporting a Bug**
The Plugin can raise a GitHub issue for a failed test case. This happens only when you press **Report Bug** and confirm
the dialog it opens.

* **What is sent:** the test case's description and expected result, what you recorded as the actual result, the
  exception or stacktrace you captured, and any screenshots you pasted into the failure form.
* **Where it goes:** the GitHub repository your own `testin.yml` names, and nowhere else.
* **Who sends it:** you do. The Plugin calls the GitHub CLI (`gh`) already installed and signed in on your machine, so
  the issue is raised under your own GitHub account and is subject to GitHub's own terms and privacy policy.
* **What the Developer receives:** nothing. No copy is kept, no server of the Developer's is involved, and the traffic
  is between your machine and GitHub.

**4. Asking an Agent to Write a Test Method Body**
The Plugin can ask an AI coding agent that you have installed and signed in yourself to write the body of a generated
test method. This happens only when you press **Automate Test Case** and only while an agent is configured on the
Settings page; with no agent configured, nothing is sent and nothing runs.

* **What is sent:** one test case - its description, expected result, steps, test data, pre-conditions and module,
  where it sits in the Testin tree and the name of the class its method is written into - filled into a prompt you can
  read and change on the Settings page. Nothing else is sent: not your other test cases, not the code of the class the
  method sits in, not your test data folder, not your repository.
* **Where it goes:** to the command you named, running on your own machine, and from there wherever that agent sends
  it, under your own account with that provider and subject to their terms and privacy policy.
* **Who sends it:** you do. The Plugin starts the agent's own command line, already installed and signed in on your
  machine.
* **Credentials:** the Plugin holds none. An agent reads its API key from your system environment; the Plugin never
  reads that value, never stores it, never writes it to its log and never puts it on a command line.
* **What the Developer receives:** nothing. No copy is kept, no server of the Developer's is involved.

**5. Third-Party Services**
The Plugin does not integrate with or send your data to any external third-party analytics, marketing, or tracking
services.

**6. Changes to This Privacy Policy**
The Developer reserves the right to update this Privacy Policy at any time. Any changes will be reflected by an updated
"Effective Date" at the top of this document. Continued use of the Plugin after any modifications indicates your
acknowledgment of the updated Privacy Policy.

**7. Contact Information**
If you have any questions, concerns, or requests regarding your privacy and the "Test in" plugin, please contact:

* **Email:** mtb550@gmail.com
* **LinkedIn:** https://www.linkedin.com/in/mtb550