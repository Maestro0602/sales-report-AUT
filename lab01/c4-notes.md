# C4 Architecture Notes

## C4 Context Diagram Notes
1. **Deliberately omitted:** The context diagram deliberately does not show internal software structure, individual services, database technologies, or file storage protocols within the Angkor Mart Sales Report System.
2. **Quality attributes served:** The boundary serves QA-5 (Security) by establishing explicit access boundaries between internal roles (Analyst vs. Branch Manager) and external systems.
3. **Open question for client:** Does the Finance Director require an interactive dashboard view in addition to the automated email notification when reports are generated?

## C4 Container Diagram Notes
1. **Deliberately omitted:** The container diagram deliberately does not show internal component code structure (such as Java packages `model`, `ingest`, and `render`), thread models, or memory allocation details.
2. **Quality attributes served:** The `Landing Zone` container directly supports QA-3 (Availability) by decoupling file arrival and network delays from processing, while the `Report Batch` container directly serves QA-1 (Performance) by running batch aggregation in an isolated 8-core batch job without web request overhead.
3. **Open question for client:** What SFTP credentials or authentication mechanism should be configured for the branch POS systems uploading files to the Landing Zone?
