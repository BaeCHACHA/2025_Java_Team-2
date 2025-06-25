# 2025_Java_Team-2
2025 Java Team Project team 2 - mini git hub / mini cloud / source code sharing platform

---

## 👥 Team Members

- **강승윤**
- **김우현**
- **박현욱**
- **배지훈**
- **이동훈**

---

## 🛠 개발 환경

- **OS**: Windows 11
- **Language**: Java
- **IDE**: Eclipse IDE
- **DB**: MySQL 8.0
- **빌드 도구**: 없음 (jar 파일 수동 포함)

---

## 📦 라이브러리 목록

- `commons-logging-1.2`
- `HikariCP-5.1.0`
- `mysql-connector-j-9.3.0`
- `slf4j-api-2.0.9`, `slf4j-simple-2.0.9`
- `spring-security-crypto-6.4.4`

---

## 📚 주요 기능

- 사용자 로그인 및 회원가입 (비밀번호 해시 처리)
- 그룹 생성 / 참가 (참가코드 시스템)
- 페이지 생성 및 수정 기록 저장
- 파일 업로드, 다운로드, 수정
- 리비전 관리 및 댓글 기능

* 자세한 기능은 pdf 파일을 확인해 주세요

---

## 🗂 데이터베이스 구성

- USERS  
- GROUP_DATA  
- GROUP_MEMBERSHIP  
- PAGES  
- FILE_REVISIONS  
- FILE_DATA  
- COMMENTS  

> 각 테이블에 대한 컬럼 및 관계는 `/sql/` 폴더의 스크립트와 함께 확인 가능합니다.

---

## 🔐 보안 설계

- 비밀번호는 해시 처리 (`spring-security-crypto`)
- 로그인 정보는 `.properties` 파일로 외부 접근 차단
- 예외 처리 기반의 신뢰성 있는 구조

---

## 🚀 향후 확장성

- Spring 기반 웹 버전으로의 확장 가능
- AWS S3 또는 클라우드 파일 저장 연동
- 프로젝트 팀별 문서 관리 플랫폼으로 발전 가능성 有
- 소스코드 외 다양한 종류의 파일 공유기능으로 확장 가능

---

## 🪪 라이선스

본 프로젝트는 MIT License로 배포됩니다.  
자세한 내용은 `LICENSE` 파일을 참고하세요.
------------------------------------------------------------------------------------------------

## 🛠 Development Environment

- **OS**: Windows 11  
- **Language**: Java  
- **IDE**: Eclipse IDE  
- **Database**: MySQL 8.0  
- **Build Tool**: None (JAR files manually included)

---

## 📦 Libraries Used

- `commons-logging-1.2`  
- `HikariCP-5.1.0`  
- `mysql-connector-j-9.3.0`  
- `slf4j-api-2.0.9`, `slf4j-simple-2.0.9`  
- `spring-security-crypto-6.4.4`

---

## 📚 Key Features

- User login and registration (with password hashing)  
- Group creation and participation (via join code system)  
- Page creation with revision history tracking  
- File upload, download, and editing features  
- Revision control and comment support  

*For more detailed feature descriptions, please refer to the project PDF.*

---

## 🗂 Database Schema

- `USERS`  
- `GROUP_DATA`  
- `GROUP_MEMBERSHIP`  
- `PAGES`  
- `FILE_REVISIONS`  
- `FILE_DATA`  
- `COMMENTS`  

> Column definitions and relationships are available in the SQL scripts under the `/sql/` directory.

---

## 🔐 Security Design

- Passwords are securely hashed using `spring-security-crypto`  
- Login credentials are stored in `.properties` files to block external access  
- Comprehensive exception handling ensures a reliable system

---

## 🚀 Future Scalability

- Potential migration to a Spring-based web version  
- Integration with AWS S3 or other cloud storage services  
- Expandable into a full-featured document collaboration platform for teams
- Expandable to support sharing of various file types beyond source code

---

## 🪪 License

This project is distributed under the **MIT License**.  
See the `LICENSE` file for more details.
---
