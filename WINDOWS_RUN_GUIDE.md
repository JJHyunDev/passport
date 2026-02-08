# Windows Offline Run Guide (EXE)

## 목표
- PaddleOCR 모델을 미리 다운로드해서 EXE가 **네트워크 없이** 동작하도록 구성
- 로컬에서 OCR → Excel 다운로드까지 검증

## 사전 준비
- Windows 10/11 (x64)
- JDK 17 또는 21 설치 (예: Corretto/Temurin)
- Python 3.8~3.12 (64-bit)

## 1) Python 런타임 포함(설치형 EXE)
사용자에게는 **EXE 하나만 배포**하고 싶다면, Python 런타임까지 프로젝트에 내장합니다.

PowerShell에서 프로젝트 루트 기준:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-python-runtime.ps1
```

이 스크립트는 `python/` 폴더를 만들고, PaddleOCR 의존성을 설치합니다.

## 2) OCR 모델 다운로드 (1회)
모델은 오프라인 실행을 위해 미리 받아둡니다.

```powershell
.\python\python.exe ocr-worker\download_models.py --model-dir ocr-models --disable-source-check
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

## 4) EXE 패키징 (설치형)
```powershell
.\gradlew.bat bootJar
.\scripts\package-exe.ps1
```

출력:
- `dist\PassportOcr-<version>.exe` (최종 배포 파일)

설치형 EXE 내부에 다음이 포함됩니다:
- Python 런타임 (`python/`)
- OCR 모델 (`ocr-models/`)
- 설정 파일 (`application.yml`)

## 5) 완전 오프라인 실행
- 최종 배포는 **EXE 하나만 전달**하면 됩니다.
- 설치 후 실행은 인터넷 연결 없이 동작합니다.

## UI 확인
- 기본값으로 실행 시 브라우저가 자동으로 열립니다.
- 자동으로 열리지 않으면 `http://localhost:8080` 으로 접속하세요.

## 문제 해결
- Python 경로가 다르면 `application.yml`의 `ocr.worker.python`을 변경
- 모델 경로가 다르면 `ocr.worker.model-dir` 수정
