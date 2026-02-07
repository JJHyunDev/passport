# 여권 OCR → Excel Export 프로젝트 기획

## 목표
- 여러 장의 여권 이미지를 업로드하고 OCR로 내용을 추출
- DB 저장 없이 처리
- 결과를 Excel(.xlsx) 파일로 다운로드

## 핵심 요구사항
- **다중 이미지 업로드** 지원
- **OCR 처리** 후 여권 정보 추출
- **Excel 파일 생성 및 다운로드**
- **DB 미사용** (이미지/결과 저장하지 않음)
- **관리자 로그인**만 허용 (회원가입 불가)
- **배포 고려** (보안/성능/비용)

## 기능 목록
### 0) 접근 제어
- 오프라인 EXE 기반 단일 사용자 가정
- 로그인/회원가입 없음

### 1) 이미지 업로드
- 다중 파일 선택 가능
- 업로드 후 미리보기/파일 리스트 표시

### 2) OCR 처리
- 업로드된 이미지에서 여권 정보 추출
- 처리 중 상태 표시 (로딩/진행률)

### 3) Excel 내보내기
- 버튼 클릭 시 Excel 생성
- 브라우저에서 파일 다운로드 제공

## 출력 데이터 (예시 컬럼)
- Passport No
- Name
- Nationality
- Date of Birth
- Sex
- Date of Issue
- Date of Expiry
- Issuing Country

## 처리 흐름 (MVP)
1. 사용자 이미지 업로드
2. 서버로 이미지 전송
3. OCR로 여권 정보 추출
4. Excel 파일 생성
5. 다운로드 응답

## 제약 및 원칙
- 이미지/결과를 서버에 저장하지 않음
- 대량 업로드를 고려한 안정성 필요
- 정확도 확보를 위한 OCR 엔진 선택 필요
- 관리자 계정 정보는 안전하게 관리 (환경변수/비밀 저장소)

## 추가 논의 필요 사항
- OCR 엔진 선택 (클라우드 vs 온프레미스)
- 필수 컬럼 확정
- 최대 업로드 수/파일 크기 제한
- 에러/예외 처리 UX
- 기술 스택 확정 (Spring Boot)
- 인증 방식 확정 (로그인 없음)
- 개발 순서 (백엔드 먼저)
- 배포 방식 (AWS 최소 구성 vs 오프라인 EXE)

---

## OCR 엔진 옵션 비교
### 클라우드 OCR (정확도 우선)
- 장점: 정확도 높음, 유지보수 부담 낮음, 빠른 적용
- 단점: 비용 발생, 네트워크/개인정보 이슈 고려 필요
- 후보: AWS Textract, Google Vision OCR, Azure Document Intelligence

### 온프레미스 OCR (비용/통제 우선)
- 장점: 데이터 외부 전송 없음, 오프라인 가능
- 단점: 정확도/튜닝 부담, 모델/의존성 관리 필요
- 후보: Tesseract + MRZ 파서, PaddleOCR, EasyOCR

### 선택
- **PaddleOCR + MRZ 파서**로 진행 (정확도 우선, 오프라인)

---

## API 설계 초안 (예시)
### OCR + Excel 다운로드
- `POST /api/ocr/export`
  - 요청: `multipart/form-data` (images[] 업로드)
  - 응답: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - 헤더: `Content-Disposition: attachment; filename="passport_export.xlsx"`

### 선택 (프론트에서 미리보기)
- `POST /api/ocr/preview`
  - 요청: `multipart/form-data`
  - 응답: JSON (추출된 필드 목록)

---

## 화면 흐름 (UX)
1. 업로드 화면 (다중 업로드 + 리스트)
2. 업로드 완료 후 “Excel로 내보내기” 버튼 활성화
3. 처리 진행 상태 표시
4. 완료 시 Excel 다운로드

---

## 배포 고려사항
- 로컬 실행 기준이므로 HTTPS는 선택 사항
- 업로드 제한 (파일 크기/개수)
- 요청 타임아웃/재시도 정책
- OCR 실패/일부 실패 시 처리 전략
- 로그에 민감정보 최소화

---

## 확정 사항
- 백엔드: **Spring Boot**
- 인증: **로그인 없음 (오프라인 EXE, 단일 사용자 가정)**
- 개발 순서: **백엔드 우선**
- 배포 우선순위: **오프라인 EXE (민감 데이터 보호)**
- 대상 OS: **Windows 전용**
- 최대 업로드 수: **50장**
- OCR: **PaddleOCR + MRZ 파서**

---

## 배포 옵션
### 옵션 A: AWS 최소 구성
- 목적: 인터넷 연결 가능 환경에서 운영
- 예시 구성: EC2 1대 + (필요 시) S3 임시 업로드 + CloudWatch 로그
- 장점: 운영/배포 단순, 접근성 높음
- 단점: 네트워크 필요, 비용 발생

### 옵션 B: 오프라인 실행 (EXE)
- 목적: 인터넷 없이 단독 실행
- 방법: Spring Boot + 내장 OCR(온프레미스) + 로컬 실행 패키징
- 장점: 데이터 외부 전송 없음, 네트워크 불필요
- 단점: 정확도/성능 튜닝 필요, 배포 파일 크기 증가

---

## 오프라인(보안) 설계 방향
- 모든 OCR 처리를 로컬에서 수행 (외부 API 호출 없음)
- 이미지/결과를 디스크에 저장하지 않고 메모리에서 처리
- 불가피한 임시 파일 사용 시 처리 후 즉시 삭제
- 로컬 실행 패키지: Windows EXE 중심 (필요 시 Mac/Linux 별도)

---

## 백엔드 설계 (Spring Boot, 오프라인)
### 모듈 구성
- Web API: 업로드/처리/엑셀 다운로드
- OCR Worker: PaddleOCR 실행 (로컬 Python 프로세스)
- Excel Exporter: Apache POI 기반 .xlsx 생성

### 처리 파이프라인
1. 다중 이미지 수신 (multipart)
2. 이미지 전처리 (리사이즈/회전 보정, 옵션)
3. OCR 수행 (PaddleOCR)
4. MRZ 후보 추출 (<< 포함 라인 탐지)
5. MRZ 파싱 → 필드 매핑
6. Excel 생성 후 다운로드 응답

### 임시 파일 전략
- 가능한 한 메모리 처리
- PaddleOCR가 파일 경로를 요구하면 OS Temp에 저장 후 즉시 삭제

### 장애/예외 처리
- 이미지별 OCR 실패 시 개별 오류 기록
- Excel 내 결과에 실패 사유 컬럼 추가 가능

---

## Excel 스키마 (초안)
- Passport No
- Name
- Nationality
- Date of Birth
- Sex
- Date of Issue
- Date of Expiry
- Issuing Country
- MRZ Raw
- Error (optional)

## Excel 컬럼 변경 전략 (설정 기반)
- `application.yml`에 컬럼 정의를 두고, 엑셀 생성 시 해당 설정을 순회
- 컬럼 순서 변경: 설정 리스트 순서만 변경
- 컬럼 추가/삭제: 항목 추가/삭제
- 컬럼 라벨 변경: label만 수정

---

## EXE 패키징 전략 (Windows)
- Spring Boot 실행 JAR + 내장 JRE를 `jpackage`로 EXE 생성
- Python 런타임 + PaddleOCR 모델을 함께 포함
- EXE 실행 시 로컬 서버 구동 후 `http://localhost:<port>` 자동 오픈

---

## OCR 모델 준비 (오프라인)
- 모델 파일을 `ocr-models/`에 미리 다운로드하고 패키징에 포함
- `application.yml`의 `ocr.worker.model-dir`로 경로 지정
- 다운로드 스크립트: `ocr-worker/download_models.py`
