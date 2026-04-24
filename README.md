# Mildam 🔐

> 암호화 기반 보안 채팅 서비스

텔레그램 수준의 보안을 목표로 구현한 실시간 채팅 애플리케이션입니다.
E2E 암호화, JWT 인증, WebSocket 실시간 통신을 핵심으로 합니다.

---

## 기술 스택

### 백엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 | 언어 |
| Spring Boot | 3.2.5 | 웹 프레임워크 |
| Spring Security | 6.x | 인증/인가 |
| Spring WebSocket | - | 실시간 채팅 |
| PostgreSQL | 16 | 데이터베이스 |
| Redis | 7 | JWT 블랙리스트, 세션 |
| JWT (jjwt) | 0.12.5 | 토큰 인증 |
| BouncyCastle | 1.78.1 | X25519 암호화 |
| Bucket4j | 7.6.0 | Rate Limiting |

### 프론트엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| React | 18 | UI 프레임워크 |
| TypeScript | 5.x | 타입 안전성 |
| Tailwind CSS | 3.x | 스타일링 |
| @stomp/stompjs | - | WebSocket 클라이언트 |
| Axios | - | HTTP 클라이언트 |
| React Router | 6.x | 라우팅 |

---

## 주요 기능

### 보안
- **JWT 인증** — Access Token (15분) + Refresh Token (7일) Rotation
- **Redis 블랙리스트** — 로그아웃 시 즉시 토큰 무효화
- **계정 잠금** — 로그인 5회 실패 시 30분 잠금
- **Rate Limiting** — IP/유저별 요청 횟수 제한
- **SQL Injection / XSS 방어** — 전역 보안 필터
- **보안 헤더** — HSTS, X-Frame-Options, CSP 등
- **HTTPS** — TLS 1.3

### 채팅
- **실시간 채팅** — WebSocket + STOMP 프로토콜
- **일반 채팅** — 서버 측 AES-256-GCM 암호화
- **시크릿 채팅** — X25519 ECDH 키 교환 기반 E2E 암호화
- **Key Fingerprint** — SHA-256 이모지 지문으로 MITM 탐지
- **Perfect Forward Secrecy** — 세션마다 새 키 생성
- **메시지 TTL** — 설정 시간 후 자동 삭제

### 채팅방
- **초대 코드** — 8자리 랜덤 코드로 입장
- **초대 링크** — 링크 공유로 간편 입장
- **채팅방 나가기** — 즉시 멤버 제거

### 메시지
- **읽음 확인** — 1대1은 읽음, 그룹은 읽은 사람 표시
- **메시지 삭제** — 본인 메시지 삭제
- **이미지/파일 전송** — 멀티미디어 지원

### 프로필
- **닉네임 변경** — 표시 이름 설정
- **상태 메시지** — 한 줄 소개
- **프로필 사진** — 이미지 업로드 (최대 5MB)

---

## 시작하기

### 사전 요구사항
- Java 17
- Docker Desktop
- Node.js 18+

### 1. 저장소 클론
```bash
git clone https://github.com/your-username/mildam.git
cd mildam
```

### 2. 인프라 실행 (PostgreSQL, Redis)
```bash
cd secure-chat-backend
docker-compose up -d
```

### 3. 백엔드 실행
```bash
gradlew clean bootRun
```

### 4. 프론트엔드 실행
```bash
cd frontend
npm install
npm start
```

### 5. 브라우저 접속
```
http://localhost:3000
```

---

## 아키텍처

```
클라이언트 (React)
    │
    ├── REST API (HTTP)
    │       └── Spring Boot (8080)
    │               ├── Spring Security (JWT 검증)
    │               ├── PostgreSQL (데이터)
    │               └── Redis (세션, 블랙리스트)
    │
    └── WebSocket (STOMP)
            └── Spring WebSocket
                    ├── 일반 채팅 브로드캐스트
                    └── 시크릿 채팅 (E2E 암호화)
```

---

## 보안 설계

### 일반 채팅
```
클라이언트 → HTTPS → 서버 (AES-256-GCM 암호화) → DB
```
서버가 암호화 키를 관리하며 메시지를 암호화하여 저장합니다.

### 시크릿 채팅
```
Alice ──공개키 교환──→ 서버 ──공개키 전달──→ Bob
Alice ←─────────────── 서버 ←─공개키 전달── Bob

Alice: 공유비밀키 = ECDH(Alice_개인키, Bob_공개키)
Bob:   공유비밀키 = ECDH(Bob_개인키,   Alice_공개키)
→ 수학적으로 동일한 공유 비밀키 도출
→ 서버는 공유 비밀키를 절대 알 수 없음
```

---

## 환경 변수

```env
# PostgreSQL
DB_PASSWORD=your_password

# Redis
REDIS_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key_minimum_32_characters
```

---

## 프로젝트 구조

```
mildam/
├── secure-chat-backend/          # Spring Boot 백엔드
│   ├── src/main/java/com/securechat/secure_chat/
│   │   ├── auth/                 # 인증/인가
│   │   ├── chat/                 # 채팅 기능
│   │   ├── domain/               # JPA 엔티티
│   │   └── security/             # 보안 설정
│   ├── docker-compose.yml
│   └── build.gradle
│
└── frontend/                     # React 프론트엔드
    ├── src/
    │   ├── api/                  # API 호출
    │   ├── hooks/                # 커스텀 훅
    │   ├── pages/                # 페이지 컴포넌트
    │   └── types/                # TypeScript 타입
    └── package.json
```

---

## 라이선스

MIT License © 2026 Mildam
