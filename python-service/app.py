from collections import Counter
from math import sqrt
from typing import List

from fastapi import FastAPI
from pydantic import BaseModel

try:
    import spacy

    try:
        NLP = spacy.load("en_core_web_sm")
    except Exception:
        NLP = spacy.blank("en")
except Exception:
    NLP = None

app = FastAPI(title="Resume Screening NLP Service")

STOP_WORDS = {
    "for", "with", "and", "the", "a", "an", "looking", "to", "of", "in", "on",
    "at", "by", "from", "or", "as", "is", "are", "be", "will", "should", "can"
}

KNOWN_SKILLS = [
    "java", "spring", "spring boot", "mysql", "sql", "python", "django", "api",
    "rest", "hibernate", "jpa", "javascript", "html", "css", "react", "docker",
    "microservices", "kafka", "aws", "git", "bootstrap", "thymeleaf", "mongodb",
    "postgresql", "linux", "excel", "machine learning"
]

SKILL_WEIGHT = 0.45
KEYWORD_WEIGHT = 0.30
SEMANTIC_WEIGHT = 0.25


class AnalysisRequest(BaseModel):
    resumeText: str
    jobDescription: str


class AnalysisResponse(BaseModel):
    finalScore: float
    skillScore: float
    keywordScore: float
    semanticScore: float
    matchedSkills: List[str]
    detectedSkills: List[str]
    explanation: str
    analysisSource: str


def normalize(text: str) -> str:
    cleaned = "".join(ch.lower() if ch.isalnum() or ch in {"+", "#", ".", " "} else " " for ch in (text or ""))
    return " ".join(cleaned.split())


def tokenize(text: str) -> List[str]:
    normalized = normalize(text)
    if not normalized:
        return []

    if NLP is not None:
        doc = NLP(normalized)
        raw_tokens = [
            token.lemma_.lower() if token.lemma_ else token.text.lower()
            for token in doc
            if not token.is_space and token.text.lower() not in STOP_WORDS and len(token.text.strip()) > 1
        ]
        tokens: List[str] = []
        for tok in raw_tokens:
            if not tok:
                continue

            # Synonym normalization for skill matching + token similarity.
            if tok == "js":
                tokens.append("javascript")
            elif tok == "reactjs":
                tokens.append("react")
            elif tok == "spring":
                tokens.append("spring")
                tokens.append("boot")
            else:
                tokens.append(tok)

        return tokens

    # Fallback tokenization.
    tokens: List[str] = []
    for token in normalized.split():
        if token in STOP_WORDS or len(token) <= 1:
            continue

        if token == "js":
            tokens.append("javascript")
        elif token == "reactjs":
            tokens.append("react")
        elif token == "spring":
            tokens.append("spring")
            tokens.append("boot")
        else:
            tokens.append(token)
    return tokens


def canonicalize_skill_name(skill: str) -> str:
    if skill == "spring":
        return "spring boot"
    return skill


def contains_consecutive_tokens(tokens: List[str], required: List[str]) -> bool:
    if not required or not tokens or len(required) > len(tokens):
        return False

    for i in range(0, len(tokens) - len(required) + 1):
        if tokens[i:i + len(required)] == required:
            return True
    return False


def extract_skills(text: str) -> List[str]:
    tokens = tokenize(text)

    detected: List[str] = []
    seen = set()
    for skill in KNOWN_SKILLS:
        canonical_skill = canonicalize_skill_name(skill)
        if canonical_skill in seen:
            continue

        skill_tokens = [t for t in canonical_skill.split() if t]
        if not skill_tokens:
            continue

        if len(skill_tokens) == 1:
            matched = skill_tokens[0] in tokens
        else:
            matched = contains_consecutive_tokens(tokens, skill_tokens)

        if matched:
            detected.append(canonical_skill)
            seen.add(canonical_skill)

    return detected


def keyword_score(resume_tokens: List[str], job_tokens: List[str]) -> float:
    job_keywords = list(dict.fromkeys(job_tokens))
    if not job_keywords:
        return 0.0
    resume_keyword_set = set(resume_tokens)
    overlap = sum(1 for token in job_keywords if token in resume_keyword_set)
    return (overlap / len(job_keywords)) * 100


def semantic_score(resume_tokens: List[str], job_tokens: List[str]) -> float:
    if not resume_tokens or not job_tokens:
        return 0.0

    resume_freq = Counter(resume_tokens)
    job_freq = Counter(job_tokens)
    vocabulary = set(resume_freq) | set(job_freq)

    dot = sum(resume_freq[token] * job_freq[token] for token in vocabulary)
    resume_mag = sqrt(sum(value * value for value in resume_freq.values()))
    job_mag = sqrt(sum(value * value for value in job_freq.values()))

    if not resume_mag or not job_mag:
        return 0.0

    return (dot / (resume_mag * job_mag)) * 100


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/analyze", response_model=AnalysisResponse)
def analyze(request: AnalysisRequest) -> AnalysisResponse:
    resume_tokens = tokenize(request.resumeText)
    job_tokens = tokenize(request.jobDescription)

    resume_skills = extract_skills(request.resumeText)
    job_skills = extract_skills(request.jobDescription)
    matched_skills = [skill for skill in job_skills if skill in resume_skills]

    skill_score = 0.0 if not job_skills else (len(matched_skills) / len(job_skills)) * 100
    key_score = keyword_score(resume_tokens, job_tokens)
    sem_score = semantic_score(resume_tokens, job_tokens)
    final_score = (skill_score * SKILL_WEIGHT) + (key_score * KEYWORD_WEIGHT) + (sem_score * SEMANTIC_WEIGHT)

    if not job_skills:
        explanation = (
            "Python NLP fallback used keyword and token similarity because no known job skills were detected. "
            f"Keyword score {key_score:.0f}%, token similarity {sem_score:.0f}%."
        )
    else:
        matched = ", ".join(matched_skills) if matched_skills else "none"
        explanation = (
            "Python NLP score = 45% skill match + 30% keyword match + 25% token similarity. "
            f"Matched {len(matched_skills)}/{len(job_skills)} job skills ({matched}). "
            f"Skill score {skill_score:.0f}%, keyword score {key_score:.0f}%, token similarity {sem_score:.0f}%."
        )

    return AnalysisResponse(
        finalScore=final_score,
        skillScore=skill_score,
        keywordScore=key_score,
        semanticScore=sem_score,
        matchedSkills=matched_skills,
        detectedSkills=resume_skills,
        explanation=explanation,
        analysisSource="python_nlp",
    )
