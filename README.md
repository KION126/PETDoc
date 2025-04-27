## 📁프로젝트 구조

```
PETDoc/
 ├── app/
 │    ├── src/
 │    │    └── main/
 │    │          ├── java/com/petdoc/   # 자바파일
 │    │          │     ├── customView/  # 커스텀 뷰(공통 컴포넌트 개념)
 │    │          │     │     ├── TopToolbar.java    (상단 툴바)
 │    │          │     │     └── MainButton.java    (메인 페이지 버튼)
 │    │          │     │
 │    │          │     ├── main    # 메인페이지
 │    │          │     │     └── MainActivity.java
 │    │          │     │
 │    │          │     └── auth/   # 로그인 프로세스 페이지
 │    │          │           ├── AuthActivity.java
 │    │          │           └── SettingNameActivity.java
 │    │          │     
 │    │          ├── res/
 │    │          │     ├── layout/      # 페이지XML
 │    │          │     │     ├── customView/  # 커스텀 뷰(공통 컴포넌트 개념)
 │    │          │     │     │     ├── view_top_toolbar.xml
 │    │          │     │     │     └── view_main_button.xml
 │    │          │     │     │
 │    │          │     │     ├── main
 │    │          │     │     │     └── activity_main.xml
 │    │          │     │     │
 │    │          │     │     └── auth/
 │    │          │     │           ├── activity_auth.xml
 │    │          │     │           ├── activity_setting_name.xml
 │    │          │     │           └── activity_setting_gender.xml
 │    │          │     │
 │    │          │     ├── font/        # 폰트 파일(Notosans-kr)
 │    │          │     │     ├── notosanskr_medium.ttf
 │    │          │     │     └── notosanskr_bold.ttf
 │    │          │     │
 │    │          │     ├── drawable/    # 배경, 버튼 이미지
 │    │          │     ├── mipmap/      # 아이콘
 │    │          │     └── values/      # 환경 설정
 │    │          │     │     ├── colors.xml    # 색상 정의
 │    │          │     │     ├── strings.xml   # 문자열(텍스트) 정의
 │    │          │     │     └── themes.xml    # 기본 디자인 설정 정의(기본 글꼴)
 │    │          │     │
 │    │          └── AndroidManifest.xml
 │    │
 │    │
 │    └── build.gradle (Module)
 ├── build.gradle (Project)
 ├── gradle/
 ├── .idea/
 ├── .gradle/
 ├── local.properties
 └── settings.gradle
```
