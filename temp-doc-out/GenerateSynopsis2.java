import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

public class GenerateSynopsis2 {

    private static final Path ROOT = Paths.get("").toAbsolutePath();
    private static final Path DIAGRAMS = ROOT.resolve("temp-doc-out").resolve("generated-diagrams");
    private static final Path OUTPUT = ROOT.resolve("Synopsis_2_GNDU_Final_Resume_Screening.docx");

    public static void main(String[] args) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        configurePage(doc);
        addFooter(doc);

        titlePage(doc);

        addSection(doc, "1. Introduction",
                "Recruitment has become increasingly data-heavy, especially for roles where a single opening can attract dozens or even hundreds of applications. In such situations, the first level of screening is repetitive and time consuming. Recruiters must manually open multiple files, skim through resumes in different formats, compare candidates against a job description, and then prepare a shortlist. This approach is slow, inconsistent, and vulnerable to human oversight.",
                "The proposed project, titled Intelligent Automated Resume Screening & Ranking System, addresses this problem by building a practical end-to-end platform for resume analysis and candidate ranking. The system combines a recruiter-facing dashboard, document parsing through Apache Tika, backend orchestration through Spring Boot, persistent storage in MySQL, and an NLP layer through Python. The objective is not to replace human judgment, but to reduce repetitive screening work and present the recruiter with a ranked, explainable view of candidates.",
                "Compared with a basic keyword filter, the present system moves one step ahead by combining extracted skills, keyword overlap, and contextual token similarity. This makes the system more useful for real academic demonstration as well as for practical recruitment scenarios where the same skill may be written in different forms.");

        addSection(doc, "2. Problem Statement",
                "In the current recruitment workflow, resumes are received in different formats such as PDF, DOCX, and TXT. Manually reading and comparing these documents against a job description requires time, consistency, and domain understanding. Traditional ATS tools often depend on rigid keyword matching. As a result, they may reject suitable candidates simply because the wording in the resume does not exactly match the wording in the job posting.",
                "This creates a real-world problem with both technical and human impact. Recruiters lose time, organizations delay hiring, and qualified candidates may be missed. The project therefore focuses on building a screening system that can automatically extract resume text, process it, compare it with job requirements, and return a ranked result with a clear explanation that supports decision making.");

        addHeading(doc, "3. Detailed Proposed Solution");
        addBody(doc, "The proposed solution is designed as a hybrid architecture in which the web interface and core application flow are handled by Spring Boot, while the intelligent text analysis layer is handled by Python.");
        addCode(doc,
                "Resume Upload -> File Validation -> Text Extraction -> NLP / Skill Processing",
                "-> Ranking Computation -> Database Storage -> Dashboard Output");
        addBody(doc, "First, the recruiter enters a job title, uploads candidate resumes, and provides a job description through the dashboard. The Spring Boot backend validates the files and uses Apache Tika to extract text from supported formats. The extracted text is then normalized and prepared for analysis.");
        addBody(doc, "Next, the backend sends the prepared content to the Python NLP microservice. This service performs lightweight language processing using spaCy and NLTK, identifies relevant skills, and computes analysis measures such as skill overlap, keyword match, and token similarity. The final ranking is generated as a weighted score.");
        addBody(doc, "Once the score is generated, the results are stored in MySQL and displayed through the result dashboard. The recruiter can immediately see which resume is a stronger match, what skills were found, and how the final score was calculated.");
        addBody(doc, "This solution is better than a traditional ATS in three practical ways. First, it supports multiple document formats and reduces dependency on manual copy-paste. Second, it uses a richer ranking approach instead of a simple binary keyword filter. Third, it presents the result in a transparent way so the recruiter can understand why one candidate scored higher than another.");

        addSection(doc, "4. Complete Working Pipeline",
                "The present implementation follows a consistent operational flow from input to output. The recruiter starts from the dashboard and enters a job title along with the target job description. Two resumes can be uploaded for direct comparison in the current working demo.",
                "The files are accepted by the Spring Boot layer and passed to Apache Tika, which extracts raw text from PDF, DOCX, and TXT documents. The resulting content is cleaned and forwarded to the analysis service. If the Python NLP service is available, the backend sends the extracted resume text and job description through a REST API request. If the Python service is unavailable, the Java service falls back to its local ranking logic so the screening process can still continue.",
                "Finally, the ranking result is saved into MySQL. The result page then displays a score, match category, extracted skills, comparison charts, and detailed resume text. This makes the project suitable for both a technical demonstration and a viva presentation because the flow is visible, layered, and explainable.");

        addHeading(doc, "5. Detailed System Architecture");
        addBody(doc, "The system uses a layered architecture with clearly separated responsibilities. The user interface layer is built using HTML, CSS, JavaScript, Bootstrap, Thymeleaf, and Chart.js. It collects recruiter input and displays both historical and current analysis results.");
        addBody(doc, "The application backend is implemented in Spring Boot. This layer manages request handling, validation, orchestration, REST endpoints, database interaction, and communication with the Python NLP engine. Apache Tika is integrated in this layer for robust resume text extraction.");
        addBody(doc, "The Python microservice is responsible for language-oriented analysis. Using FastAPI, spaCy, and NLTK, it extracts matched skills and calculates the scoring components used by the ranking engine. MySQL acts as the persistent storage layer for job descriptions, resumes, and ranking results.");
        addBody(doc, "The data flow begins at the dashboard, passes through the backend services, interacts with the parser and NLP engine, stores the processed data in the database, and returns a structured response to the presentation layer.");
        addImage(doc, "architecture.png", "Figure 1: Overall System Architecture of the Intelligent Automated Resume Screening & Ranking System", 6.4, 3.8);
        addBody(doc, "In this architecture, the dashboard remains thin and focused on interaction, while the backend controls all core processing. This separation keeps the system maintainable and also supports future expansion, such as authentication, report export, or multi-job management.");

        addHeading(doc, "6. Module-wise Detailed Description");
        addSubHeading(doc, "6.1 User Dashboard Module");
        addBody(doc, "Purpose: The User Dashboard Module provides the main interaction point for the recruiter. It supports job title entry, resume upload, job description submission, result viewing, and history inspection.");
        addBody(doc, "Internal Working: The dashboard is implemented using HTML, CSS, Bootstrap, Thymeleaf, and JavaScript. It sends input data to the Spring Boot backend through form submission and REST-based fetch requests. It also renders analytics through cards, badges, and chart components.");
        addBody(doc, "Technologies Used: HTML5, CSS3, Bootstrap 5, JavaScript, Thymeleaf, Chart.js.");
        addBody(doc, "Data Flow: Recruiter input -> Dashboard form -> Backend controller/API -> Analysis response -> Result cards and charts.");
        addBody(doc, "Screenshot Placement: Figure X: Main Dashboard UI, Figure X: Dashboard showing latest analysis preview.");

        addSubHeading(doc, "6.2 Resume Upload and Parsing Module");
        addBody(doc, "Purpose: This module receives uploaded resume files and converts them into machine-readable text for downstream analysis.");
        addBody(doc, "Internal Working: The Spring Boot service validates file input and uses Apache Tika to parse PDF, DOCX, and TXT files. The extracted text is preserved for ranking as well as for record storage.");
        addBody(doc, "Technologies Used: Spring Boot, MultipartFile handling, Apache Tika.");
        addBody(doc, "Data Flow: Uploaded file -> Validation -> Apache Tika parser -> Extracted plain text -> Analysis service and database.");
        addBody(doc, "Screenshot Placement: Figure X: Resume upload screen, Figure X: Extracted text output in result view.");

        addSubHeading(doc, "6.3 NLP Processing Module");
        addBody(doc, "Purpose: This module adds intelligence to the screening process by analyzing the extracted text with respect to the job description.");
        addBody(doc, "Internal Working: The Spring Boot backend sends resume text and job description to a Python FastAPI microservice. The Python service uses spaCy and NLTK when available, and a simple fallback routine otherwise. It identifies matched skills and returns structured scoring data.");
        addBody(doc, "Technologies Used: Python, FastAPI, spaCy, NLTK, REST API communication.");
        addBody(doc, "Data Flow: Cleaned text -> REST request -> Python NLP service -> matched skills and scoring signals -> Java backend.");
        addBody(doc, "Screenshot Placement: Figure X: Python API response sample, Figure X: Matched skills section in dashboard.");

        addSubHeading(doc, "6.4 Ranking Algorithm Module");
        addBody(doc, "Purpose: The Ranking Algorithm Module generates a meaningful score for each resume relative to the job description.");
        addBody(doc, "Internal Working: The system calculates skill match percentage, keyword overlap, and token similarity. These are combined through a weighted formula to generate the final score. The module also prepares an explanation so that the output is not a black box.");
        addBody(doc, "Technologies Used: Java service layer, Python NLP layer, weighted scoring logic, token analysis.");
        addBody(doc, "Data Flow: Resume text + job description -> scoring components -> final weighted score -> ranking result.");
        addBody(doc, "Screenshot Placement: Figure X: Ranking comparison cards, Figure X: score chart and explanation view.");

        addSubHeading(doc, "6.5 Database Module");
        addBody(doc, "Purpose: This module stores project data so that analyses are not lost after one session.");
        addBody(doc, "Internal Working: MySQL stores job descriptions, resumes, and ranking results through JPA entities and repositories in the Spring Boot application. The database also supports history APIs used by the dashboard.");
        addBody(doc, "Technologies Used: MySQL, Spring Data JPA, Hibernate.");
        addBody(doc, "Data Flow: Processed objects -> entity mapping -> MySQL tables -> retrieval for history and reports.");
        addBody(doc, "Screenshot Placement: Figure X: MySQL tables, Figure X: ranking history section.");

        addHeading(doc, "7. Working of the System (Step-by-Step)");
        addBody(doc, "Step 1: The recruiter opens the dashboard and enters a job title and job description.");
        addBody(doc, "Step 2: Resume files are uploaded through the interface.");
        addBody(doc, "Step 3: The Spring Boot backend validates the files and uses Apache Tika to extract text.");
        addBody(doc, "Step 4: Extracted text is cleaned and prepared for analysis.");
        addBody(doc, "Step 5: The backend sends the data to the Python NLP service, or uses Java fallback logic if needed.");
        addBody(doc, "Step 6: The system identifies skill overlap, keyword relevance, and token-level similarity.");
        addBody(doc, "Step 7: A weighted score is calculated for each candidate.");
        addBody(doc, "Step 8: The data is stored in MySQL tables for future retrieval.");
        addBody(doc, "Step 9: The final ranked output is shown on the dashboard with charts and explanations.");
        addBody(doc, "In simple terms, the algorithm works by checking how much of the resume matches the job requirement from different angles. A resume may not contain every exact keyword, but if it contains related skills and strong contextual overlap, the final score still reflects that strength. This makes the output more practical than a basic yes-or-no filter.");

        addHeading(doc, "8. Data Flow Diagrams (DFD)");
        addSubHeading(doc, "8.1 DFD Level 0");
        addBody(doc, "Level 0 gives the highest-level view of the system. It shows the recruiter as the external actor, the resume screening system as the main process, and the supporting interaction with the database and NLP engine.");
        addImage(doc, "dfd0.png", "Figure 2: DFD Level 0", 6.2, 3.4);
        addBody(doc, "At this level, the user supplies resume files and a job description, the system performs screening, and the ranked output is returned. The system also communicates with the database for storage and with the Python service for analysis.");

        addSubHeading(doc, "8.2 DFD Level 1");
        addBody(doc, "Level 1 breaks the system into major processes such as upload, parsing, NLP processing, ranking, and dashboard output. It highlights the major data stores involved in the implementation.");
        addImage(doc, "dfd1.png", "Figure 3: DFD Level 1", 6.4, 3.9);
        addBody(doc, "This diagram explains how input data moves from the recruiter to the upload process, then into parsing, analysis, ranking, storage, and reporting. It is useful for understanding the modular design of the project.");

        addSubHeading(doc, "8.3 DFD Level 2");
        addBody(doc, "Level 2 gives a more detailed view of the internal analysis pipeline, especially the document processing and ranking stages.");
        addImage(doc, "dfd2.png", "Figure 4: DFD Level 2", 6.4, 4.0);
        addBody(doc, "This level shows the internal steps of validation, extraction, cleaning, skill detection, weighted scoring, and result storage. It demonstrates that the project follows a clear analytical sequence after file upload.");

        addHeading(doc, "9. Entity Relationship Diagram");
        addBody(doc, "The database design of the system is centered around the core entities needed for screening and ranking. Recruiter and Admin are system actors at the process level, while Candidate, Resume, Job Description, and Ranking Result represent the operational data of the application.");
        addImage(doc, "er.png", "Figure 5: Entity Relationship Diagram", 6.4, 3.8);
        addBody(doc, "Relationship Summary: A recruiter creates job descriptions and initiates screening activity. A candidate owns a resume. A resume is compared against a job description. The comparison produces a ranking result that stores the final score and related metrics. This design supports history tracking and future expansion such as multiple screening sessions.");

        addHeading(doc, "10. Use Case Diagram");
        addBody(doc, "The use case diagram identifies the major actors and their interactions with the system. In the present project, the Recruiter is the primary actor, while the Admin actor covers maintenance and record management activities expected in an extended deployment.");
        addImage(doc, "usecase.png", "Figure 6: Use Case Diagram", 6.4, 3.8);
        addBody(doc, "The recruiter uploads resumes, enters job descriptions, runs screening, views ranked results, and reviews history. The admin role manages stored data and supervises the availability of records and system-level operations.");

        addHeading(doc, "11. Flowcharts");
        addSubHeading(doc, "11.1 Resume Processing Flow");
        addImage(doc, "resume_processing_flow.png", "Figure 7: Resume Processing Flowchart", 5.6, 7.2);
        addBody(doc, "This flowchart explains how the system accepts resumes, validates them, extracts text, and prepares them for further analysis.");
        addSubHeading(doc, "11.2 Ranking Algorithm Flow");
        addImage(doc, "ranking_algorithm_flow.png", "Figure 8: Ranking Algorithm Flowchart", 5.6, 6.6);
        addBody(doc, "This flowchart shows how the score is generated from different matching components and how the final explanation is produced.");
        addSubHeading(doc, "11.3 Overall System Flow");
        addImage(doc, "system_flow.png", "Figure 9: Overall System Flowchart", 5.6, 6.6);
        addBody(doc, "This flowchart presents the complete journey from dashboard interaction to database storage and final dashboard output.");

        addHeading(doc, "12. Screenshot Placeholders");
        addBody(doc, "Figure 10: Dashboard UI");
        addItalic(doc, "[Insert screenshot of main recruiter dashboard with upload form and latest analysis panel here.]");
        addBody(doc, "Figure 11: Ranking Results Page");
        addItalic(doc, "[Insert screenshot of the full result dashboard with score cards, charts, and matched skills here.]");
        addBody(doc, "Figure 12: MySQL History View / Table Snapshot");
        addItalic(doc, "[Insert screenshot of MySQL tables or project database records here.]");
        addBody(doc, "Figure 13: API or Python NLP Response Snapshot");
        addItalic(doc, "[Insert screenshot of analysis API response, Postman output, or Python NLP terminal response here.]");

        addHeading(doc, "13. Overall Project Progress");
        addBody(doc, "The project has moved beyond the conceptual stage and currently stands at an advanced implementation level. The working Spring Boot backend is complete for resume upload, parsing, ranking orchestration, and result rendering. Apache Tika integration is complete and supports extraction from the target resume formats used in the demo. MySQL persistence is also active, and analysis history can be retrieved through dedicated APIs.");
        addBody(doc, "The Python microservice has been integrated in a practical form using FastAPI. It already supports analysis requests and returns structured scoring output. At the same time, the Java backend contains fallback logic so that the project remains demonstrable even if the Python service is unavailable during a presentation.");
        addBody(doc, "The recruiter dashboard and result dashboard are implemented and visually improved. REST endpoints for analysis and history retrieval are available. Basic test coverage has also been added for major controller flows.");
        addBody(doc, "Work currently in progress includes refinement of NLP quality, stronger skill taxonomy, and richer history and reporting experience.");
        addBody(doc, "Remaining work before final submission includes final screenshot collection, end-to-end testing with selected sample resumes, minor usability polish, and final report finishing. This progress status is realistic and reflects the present development stage of the project.");

        addHeading(doc, "14. Literature Overview");
        addBody(doc, "A review of current recruitment technology shows that many Applicant Tracking Systems are optimized for volume, but not necessarily for context. Their most common limitation is dependence on direct keyword presence. If the exact wording in the resume does not match the job description, the candidate may be under-ranked even if the actual profile is strong.");
        addBody(doc, "Research trends and practical industry experience both suggest that NLP can improve this situation by analyzing text more intelligently. Skill extraction, token normalization, and contextual similarity help the system go beyond rigid phrase matching. For an academic project, even a modest NLP pipeline is valuable because it demonstrates the shift from document storage to text understanding.");
        addBody(doc, "Another important observation from current practice is the value of hybrid architecture. Java Spring Boot offers stability, clear API structure, and strong database integration, while Python offers speed and flexibility for NLP experimentation. By combining these technologies through web APIs, the project remains modular, realistic, and extensible.");

        addHeading(doc, "15. Conclusion and Future Work");
        addBody(doc, "The Intelligent Automated Resume Screening & Ranking System addresses a relevant and practical problem in modern recruitment. It reduces manual effort, supports multiple file formats, introduces explainable ranking, and uses a structured hybrid architecture that fits well within the academic objectives of an MCA final year project.");
        addBody(doc, "At the current stage, the project already demonstrates the core pipeline successfully: recruiter input, document parsing, analysis, ranking, storage, and dashboard presentation. This makes Synopsis-2 a realistic progress submission rather than a purely planned document.");
        addBody(doc, "Future work may include stronger named entity recognition for education and experience, user authentication, batch screening for larger candidate pools, downloadable reports, and improved ranking accuracy through a richer skill knowledge base or more advanced semantic similarity models.");

        try (FileOutputStream out = new FileOutputStream(OUTPUT.toFile())) {
            doc.write(out);
        }
        doc.close();
        System.out.println(OUTPUT);
    }

    private static void configurePage(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr() ? doc.getDocument().getBody().getSectPr() : doc.getDocument().getBody().addNewSectPr();
        CTPageMar mar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        mar.setLeft(BigInteger.valueOf(1872));
        mar.setRight(BigInteger.valueOf(1440));
        mar.setTop(BigInteger.valueOf(1440));
        mar.setBottom(BigInteger.valueOf(1440));
        sectPr.addNewPgSz().setOrient(STPageOrientation.PORTRAIT);
    }

    private static void addFooter(XWPFDocument doc) {
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(doc);
        XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
        XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.getCTP().addNewFldSimple().setInstr("PAGE \\\\* MERGEFORMAT");
    }

    private static void titlePage(XWPFDocument doc) {
        addCentered(doc, "GURU NANAK DEV UNIVERSITY, AMRITSAR", 20, true, false, 300);
        addCentered(doc, "Department of Computer Science", 12, false, false, 120);
        addCentered(doc, "SYNOPSIS - 2", 20, true, false, 300);
        addCentered(doc, "for the MCA Final Year Major Project", 12, false, false, 120);
        addCentered(doc, "Intelligent Automated Resume Screening & Ranking System", 16, true, true, 240);
        addCentered(doc, "Submitted by", 12, false, false, 120);
        addCentered(doc, "Name: ________________________________", 12, false, false, 60);
        addCentered(doc, "Roll No.: ________________________________", 12, false, false, 60);
        addCentered(doc, "Registration No.: ________________________________", 12, false, false, 120);
        addCentered(doc, "Submitted to", 12, false, false, 120);
        addCentered(doc, "Project Guide: ________________________________", 12, false, false, 60);
        addCentered(doc, "Department of Computer Science, GNDU Amritsar", 12, false, false, 60);
        addCentered(doc, "Session: 2025-2026", 12, false, false, 180);
        doc.createParagraph().setPageBreak(true);
    }

    private static void addSection(XWPFDocument doc, String heading, String... paragraphs) {
        addHeading(doc, heading);
        for (String paragraph : paragraphs) {
            addBody(doc, paragraph);
        }
    }

    private static void addHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBetween(1.5);
        p.setSpacingBefore(240);
        p.setSpacingAfter(240);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(14);
        r.setBold(true);
        r.setUnderline(UnderlinePatterns.SINGLE);
        r.setText(text);
    }

    private static void addSubHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBetween(1.5);
        p.setSpacingBefore(180);
        p.setSpacingAfter(120);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(12);
        r.setBold(true);
        r.setText(text);
    }

    private static void addBody(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setVerticalAlignment(TextAlignment.AUTO);
        p.setSpacingBetween(1.5);
        p.setSpacingBefore(120);
        p.setSpacingAfter(120);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(12);
        r.setText(text);
    }

    private static void addItalic(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBetween(1.5);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(12);
        r.setItalic(true);
        r.setText(text);
    }

    private static void addCentered(XWPFDocument doc, String text, int size, boolean bold, boolean underline, int after) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBetween(1.5);
        p.setSpacingAfter(after);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(size);
        r.setBold(bold);
        if (underline) {
            r.setUnderline(UnderlinePatterns.SINGLE);
        }
        r.setText(text);
    }

    private static void addCode(XWPFDocument doc, String... lines) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setBorderTop(Borders.SINGLE);
        p.setBorderBottom(Borders.SINGLE);
        p.setSpacingBetween(1.2);
        for (int i = 0; i < lines.length; i++) {
            XWPFRun r = p.createRun();
            r.setFontFamily("Courier New");
            r.setFontSize(10);
            r.setText(lines[i]);
            if (i < lines.length - 1) {
                r.addBreak();
            }
        }
    }

    private static void addImage(XWPFDocument doc, String fileName, String caption, double widthInches, double heightInches) throws Exception {
        Path image = DIAGRAMS.resolve(fileName);
        if (!Files.exists(image)) {
            addItalic(doc, "[Missing diagram: " + fileName + "]");
            return;
        }
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        try (InputStream in = new FileInputStream(image.toFile())) {
            XWPFRun run = p.createRun();
            run.addPicture(in, XWPFDocument.PICTURE_TYPE_PNG, fileName, Units.toEMU(widthInches), Units.toEMU(heightInches));
        }
        XWPFParagraph cap = doc.createParagraph();
        cap.setAlignment(ParagraphAlignment.CENTER);
        cap.setSpacingAfter(160);
        XWPFRun r = cap.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(12);
        r.setText(caption);
    }
}
