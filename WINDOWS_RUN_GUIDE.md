# Windows Offline Run Guide (EXE)

## 목표
- PaddleOCR 모델을 미리 다운로드해서 EXE가 **네트워크 없이** 동작하도록 구성
- 로컬에서 OCR → Excel 다운로드까지 검증

## 사전 준비
- Windows 10/11
- JDK 17 또는 21 설치 (예: Corretto/Temurin)
- Python 3.9+ 설치

## 1) Python 의존성 설치
PowerShell에서 프로젝트 루트 기준:

```powershell
python -m pip install -r ocr-worker\requirements-min.txt
python -m pip install paddleocr==2.7.0.3 --no-deps
```

## 2) OCR 모델 다운로드 (1회)
모델은 오프라인 실행을 위해 미리 받아둡니다.

```powershell
python ocr-worker\download_models.py --model-dir ocr-models
```

다운로드 경로:
- `ocr-models\paddle`

`application.yml`에 이미 반영됨:
```yaml
ocr:
  worker:
    model-dir: "ocr-models/paddle"
```

## 3) 로컬 실행 (개발)
```powershell
set JAVA_HOME=C:\\Program Files\\Java\\jdk-21
.\gradlew.bat bootRun
```

브라우저 접속:
- `http://localhost:8080`

## 4) EXE 패키징
```powershell
.\gradlew.bat bootJar
.\scripts\package-exe.ps1
```

출력:
- `dist\PassportOcr-<version>.exe`
- `dist\ocr-models\paddle` (모델 포함)
- `dist\application.yml`

## 5) 완전 오프라인 실행
- EXE와 `dist\ocr-models` 폴더가 같은 위치에 있어야 합니다.
- 인터넷 연결 없이 실행 가능 (모델 다운로드는 이미 완료된 상태).

## 문제 해결
- Python 경로가 다르면 `application.yml`의 `ocr.worker.python`을 변경
- 모델 경로가 다르면 `ocr.worker.model-dir` 수정
