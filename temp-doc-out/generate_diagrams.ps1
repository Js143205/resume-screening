$ErrorActionPreference = "Stop"

$outDir = Join-Path $PSScriptRoot "generated-diagrams"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Add-Type -AssemblyName System.Drawing

function New-Canvas($width, $height) {
    $bmp = New-Object System.Drawing.Bitmap($width, $height)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.Color]::White)
    return @{ Bitmap = $bmp; Graphics = $g }
}

function Save-Canvas($canvas, $path) {
    $canvas.Graphics.Dispose()
    $canvas.Bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Bitmap.Dispose()
}

function Draw-Box($g, $x, $y, $w, $h, $title, $sub = "", $fillHex = "#EAF2FF") {
    $fill = [System.Drawing.ColorTranslator]::FromHtml($fillHex)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70, 110, 170), 2)
    $brush = New-Object System.Drawing.SolidBrush($fill)
    $textBrush = [System.Drawing.Brushes]::MidnightBlue
    $fontTitle = New-Object System.Drawing.Font("Times New Roman", 15, [System.Drawing.FontStyle]::Bold)
    $fontSub = New-Object System.Drawing.Font("Times New Roman", 11)
    $rect = [System.Drawing.RectangleF]::new([single]$x, [single]$y, [single]$w, [single]$h)
    $g.FillRectangle($brush, $rect)
    $g.DrawRectangle($pen, $x, $y, $w, $h)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $titleRect = [System.Drawing.RectangleF]::new([single]($x + 10), [single]($y + 12), [single]($w - 20), [single]32)
    $g.DrawString($title, $fontTitle, $textBrush, $titleRect, $sf)
    if ($sub -and $sub.Trim()) {
        $subRect = [System.Drawing.RectangleF]::new([single]($x + 10), [single]($y + 50), [single]($w - 20), [single]($h - 60))
        $g.DrawString($sub, $fontSub, [System.Drawing.Brushes]::Black, $subRect, $sf)
    }
    $fontTitle.Dispose()
    $fontSub.Dispose()
    $brush.Dispose()
    $pen.Dispose()
    $sf.Dispose()
}

function Draw-Arrow($g, $x1, $y1, $x2, $y2, $label = "") {
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::DimGray, 3)
    $pen.CustomEndCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(6, 8, $true)
    $g.DrawLine($pen, $x1, $y1, $x2, $y2)
    if ($label -and $label.Trim()) {
        $font = New-Object System.Drawing.Font("Times New Roman", 11, [System.Drawing.FontStyle]::Italic)
        $brush = [System.Drawing.Brushes]::Black
        $mx = [int](($x1 + $x2) / 2)
        $my = [int](($y1 + $y2) / 2)
        $g.DrawString($label, $font, $brush, $mx - 50, $my - 18)
        $font.Dispose()
    }
    $pen.Dispose()
}

function Draw-Actor($g, $x, $y, $label) {
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::Black, 2)
    $font = New-Object System.Drawing.Font("Times New Roman", 12, [System.Drawing.FontStyle]::Bold)
    $g.DrawEllipse($pen, $x + 22, $y, 26, 26)
    $g.DrawLine($pen, $x + 35, $y + 26, $x + 35, $y + 72)
    $g.DrawLine($pen, $x + 10, $y + 42, $x + 60, $y + 42)
    $g.DrawLine($pen, $x + 35, $y + 72, $x + 10, $y + 100)
    $g.DrawLine($pen, $x + 35, $y + 72, $x + 60, $y + 100)
    $g.DrawString($label, $font, [System.Drawing.Brushes]::Black, $x, $y + 108)
    $pen.Dispose()
    $font.Dispose()
}

# Architecture diagram
$c = New-Canvas 1400 800
$g = $c.Graphics
Draw-Box $g 70 120 220 120 "Recruiter / Admin" "Uploads resumes and views rankings" "#FFF4DA"
Draw-Box $g 360 120 250 120 "User Dashboard" "HTML, CSS, JavaScript, Thymeleaf" "#EAF2FF"
Draw-Box $g 700 120 260 120 "Spring Boot Backend" "Controllers, Services, REST APIs" "#E3F5EA"
Draw-Box $g 1040 90 250 120 "Python NLP Service" "FastAPI, spaCy, NLTK" "#F4EAFE"
Draw-Box $g 1040 260 250 120 "MySQL Database" "Resume, Job, Ranking tables" "#FFEAEA"
Draw-Box $g 700 420 260 120 "Apache Tika Parser" "PDF, DOCX, TXT extraction" "#EAF7FF"
Draw-Box $g 360 420 250 120 "Results Dashboard" "Charts, scorecards, explanations" "#F7F2FF"
Draw-Arrow $g 290 180 360 180 "input"
Draw-Arrow $g 610 180 700 180 "HTTP"
Draw-Arrow $g 960 160 1040 150 "NLP request"
Draw-Arrow $g 960 220 1040 300 "persist"
Draw-Arrow $g 830 240 830 420 "extract"
Draw-Arrow $g 700 480 610 480 "response data"
Draw-Arrow $g 485 420 485 300 "UI render"
Save-Canvas $c (Join-Path $outDir "architecture.png")

# DFD 0
$c = New-Canvas 1300 700
$g = $c.Graphics
Draw-Box $g 90 250 180 90 "Recruiter" "" "#FFF4DA"
Draw-Box $g 550 220 220 140 "Resume Screening System" "Parsing, NLP, ranking, reporting" "#EAF2FF"
Draw-Box $g 1010 140 180 90 "MySQL DB" "" "#FFEAEA"
Draw-Box $g 1010 380 180 90 "Python NLP" "" "#F4EAFE"
Draw-Arrow $g 270 295 550 295 "Resumes + JD"
Draw-Arrow $g 770 250 1010 185 "store / fetch"
Draw-Arrow $g 770 330 1010 425 "NLP analyze"
Draw-Arrow $g 550 330 270 330 "ranked output"
Save-Canvas $c (Join-Path $outDir "dfd0.png")

# DFD 1
$c = New-Canvas 1400 850
$g = $c.Graphics
Draw-Box $g 80 350 180 90 "Recruiter" "" "#FFF4DA"
Draw-Box $g 340 110 220 100 "1. Resume Upload" "accept files and JD" "#EAF2FF"
Draw-Box $g 340 270 220 100 "2. Document Parsing" "Apache Tika extraction" "#EAF7FF"
Draw-Box $g 340 430 220 100 "3. NLP Processing" "skill and token analysis" "#F4EAFE"
Draw-Box $g 340 590 220 100 "4. Ranking Engine" "weighted score generation" "#E3F5EA"
Draw-Box $g 720 190 220 100 "D1 Resume Store" "" "#FFEAEA"
Draw-Box $g 720 350 220 100 "D2 Job Store" "" "#FFEAEA"
Draw-Box $g 720 510 220 100 "D3 Ranking Store" "" "#FFEAEA"
Draw-Box $g 1080 350 220 100 "Dashboard Output" "cards, charts, explanations" "#F7F2FF"
Draw-Arrow $g 260 390 340 160 ""
Draw-Arrow $g 450 210 450 270 ""
Draw-Arrow $g 450 370 450 430 ""
Draw-Arrow $g 450 530 450 590 ""
Draw-Arrow $g 560 160 720 240 "resume data"
Draw-Arrow $g 560 160 720 400 "job details"
Draw-Arrow $g 560 640 720 560 "final scores"
Draw-Arrow $g 560 640 1080 400 "report"
Save-Canvas $c (Join-Path $outDir "dfd1.png")

# DFD 2
$c = New-Canvas 1450 900
$g = $c.Graphics
Draw-Box $g 60 390 180 90 "Recruiter" "" "#FFF4DA"
Draw-Box $g 320 90 230 90 "2.1 File Validation" "check format and size" "#EAF2FF"
Draw-Box $g 320 220 230 90 "2.2 Text Extraction" "Tika parses PDF/DOCX/TXT" "#EAF7FF"
Draw-Box $g 320 350 230 90 "2.3 Text Cleaning" "normalize and tokenize" "#F4EAFE"
Draw-Box $g 320 480 230 90 "2.4 Skill Detection" "match skill dictionary / NLP" "#E3F5EA"
Draw-Box $g 320 610 230 90 "2.5 Weighted Scoring" "skill + keyword + token similarity" "#FDF1E6"
Draw-Box $g 320 740 230 90 "2.6 Save Results" "persist resumes and scores" "#FFEAEA"
Draw-Box $g 720 220 240 100 "Python NLP Service" "optional advanced extraction" "#F4EAFE"
Draw-Box $g 720 540 240 100 "MySQL Database" "resumes, jobs, rankings" "#FFEAEA"
Draw-Box $g 1100 390 240 110 "Final Dashboard Output" "ranked candidates with explanation" "#F7F2FF"
Draw-Arrow $g 240 435 320 135 ""
Draw-Arrow $g 435 180 435 220 ""
Draw-Arrow $g 435 310 435 350 ""
Draw-Arrow $g 435 440 435 480 ""
Draw-Arrow $g 435 570 435 610 ""
Draw-Arrow $g 435 700 435 740 ""
Draw-Arrow $g 550 265 720 265 "optional API"
Draw-Arrow $g 550 785 720 590 "store"
Draw-Arrow $g 550 785 1100 445 "show results"
Save-Canvas $c (Join-Path $outDir "dfd2.png")

# ER diagram
$c = New-Canvas 1500 900
$g = $c.Graphics
Draw-Box $g 100 90 220 120 "Recruiter" "recruiter_id, name, email" "#FFF4DA"
Draw-Box $g 620 90 260 120 "Job Description" "job_id, title, description, created_at" "#EAF2FF"
Draw-Box $g 100 350 220 120 "Candidate" "candidate_id, name, email" "#E3F5EA"
Draw-Box $g 620 350 260 120 "Resume" "resume_id, file_name, extracted_text" "#EAF7FF"
Draw-Box $g 1140 350 260 120 "Ranking Result" "ranking_id, score, skill_score, keyword_score" "#FFEAEA"
Draw-Arrow $g 320 150 620 150 "creates"
Draw-Arrow $g 320 410 620 410 "owns"
Draw-Arrow $g 880 410 1140 410 "produces"
Draw-Arrow $g 750 210 750 350 "compared with"
Draw-Arrow $g 220 210 220 350 "manages"
Save-Canvas $c (Join-Path $outDir "er.png")

# Use case
$c = New-Canvas 1450 850
$g = $c.Graphics
Draw-Actor $g 70 200 "Recruiter"
Draw-Actor $g 70 500 "Admin"
$pen = New-Object System.Drawing.Pen([System.Drawing.Color]::SteelBlue, 2)
$g.DrawRectangle($pen, 280, 90, 980, 620)
$font = New-Object System.Drawing.Font("Times New Roman", 16, [System.Drawing.FontStyle]::Bold)
$g.DrawString("Resume Screening System", $font, [System.Drawing.Brushes]::MidnightBlue, 620, 105)
$font.Dispose()
foreach ($item in @(
    @{X=410;Y=180;W=230;H=58;T="Upload Resumes"},
    @{X=760;Y=180;W=250;H=58;T="Enter Job Description"},
    @{X=410;Y=310;W=230;H=58;T="Run Screening"},
    @{X=760;Y=310;W=250;H=58;T="View Ranked Results"},
    @{X=410;Y=450;W=230;H=58;T="Review History"},
    @{X=760;Y=450;W=250;H=58;T="Manage Records"}
)) {
    $g.FillEllipse([System.Drawing.Brushes]::WhiteSmoke, $item.X, $item.Y, $item.W, $item.H)
    $g.DrawEllipse($pen, $item.X, $item.Y, $item.W, $item.H)
    $f = New-Object System.Drawing.Font("Times New Roman", 12, [System.Drawing.FontStyle]::Bold)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($item.T, $f, [System.Drawing.Brushes]::Black, (New-Object System.Drawing.RectangleF($item.X, $item.Y, $item.W, $item.H)), $sf)
    $f.Dispose()
    $sf.Dispose()
}
$g.DrawLine($pen, 140, 250, 410, 210)
$g.DrawLine($pen, 140, 250, 760, 210)
$g.DrawLine($pen, 140, 250, 410, 340)
$g.DrawLine($pen, 140, 250, 760, 340)
$g.DrawLine($pen, 140, 250, 410, 480)
$g.DrawLine($pen, 140, 250, 760, 480)
$g.DrawLine($pen, 140, 550, 410, 480)
$g.DrawLine($pen, 140, 550, 760, 480)
$pen.Dispose()
Save-Canvas $c (Join-Path $outDir "usecase.png")

# Flowcharts
function Draw-Flow($name, $steps) {
    $c = New-Canvas 1200 1100
    $g = $c.Graphics
    $font = New-Object System.Drawing.Font("Times New Roman", 18, [System.Drawing.FontStyle]::Bold)
    $g.DrawString($name, $font, [System.Drawing.Brushes]::MidnightBlue, 420, 25)
    $font.Dispose()
    $y = 110
    for ($i = 0; $i -lt $steps.Count; $i++) {
        Draw-Box $g 350 $y 500 90 $steps[$i] "" "#EAF2FF"
        if ($i -lt $steps.Count - 1) {
            Draw-Arrow $g 600 ($y + 90) 600 ($y + 130) ""
        }
        $y += 130
    }
    Save-Canvas $c (Join-Path $outDir ($name.ToLower().Replace(" ", "_") + ".png"))
}

Draw-Flow "Resume Processing Flow" @(
    "Start",
    "Recruiter uploads resume files",
    "System validates format and size",
    "Apache Tika extracts text",
    "Text is cleaned and prepared",
    "Resume text is sent for analysis",
    "Structured output is returned"
)

Draw-Flow "Ranking Algorithm Flow" @(
    "Receive resume text and job description",
    "Identify matched skills",
    "Calculate keyword overlap",
    "Calculate token similarity",
    "Apply weighted scoring formula",
    "Generate explanation and rank"
)

Draw-Flow "System Flow" @(
    "User opens dashboard",
    "Uploads resumes and enters job description",
    "Spring Boot stores job data",
    "Resume parsing and NLP analysis run",
    "Scores are saved in MySQL",
    "Dashboard displays final ranked results"
)
