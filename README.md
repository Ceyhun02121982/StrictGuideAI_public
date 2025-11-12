# StrictGuide (Android MVP v0.1)

Демо-приложение под Android для «строгой» навигации слепого пользователя.
- Камера через CameraX (превью + анализатор кадров)
- Голос: STT (команды) и TTS (озвучка кратких команд)
- Вызов ChatGPT API для генерации СТРОГИХ коротких команд из JSON-саммари сцены
- Режим Calm (меньше речи), заготовка под вибро

## Быстрый старт
1. Откройте папку **StrictGuideAndroid** в Android Studio.
2. В `app/src/main/res/values/strings.xml` замените значение `openai_api_key` на свой OpenAI API Key (только для разработки!).
3. Подключите телефон (USB / adb), запустите приложение.
4. Нажмите **«Начать»** — включится камера и распознавание речи. Кнопкой **Calm** переключайте режим спокойный.

> Важно: ключ в ресурсах — только для прототипа. Для продакшена используйте серверное проксирование ключа или SafetyNet/Play Integrity.

## Где добавлять «глаз» (анализ сцены)
В `MainActivity.kt`, внутри `ImageAnalysis.setAnalyzer`, место помечено TODO.
- Подключите **ML Kit Object Detection** или **MediaPipe** (on-device).
- Сформируйте JSON-саммари (свободный коридор, препятствия, дверь, человек и т.п.).
- Вызывайте `requestStrictCommand(summaryJson)` при нужных событиях.

## Формат JSON (пример)
```json
{
  "time": 1730395200,
  "cam": "rear",
  "obstacles": [{"type":"doorway","bearing":"right","distance_m":1.8}],
  "free_path": {"bearing":"left","width_m":0.9,"length_m":3.0},
  "risk":"low",
  "mode":"calm"
}
```

## Правила команды (System Prompt)
- Максимум 6 слов.
- «Стоп» при сомнениях или риске.
- Разрешённые типы фраз: шаг/поворот/осторожность/угол головы.

## TODO (следующие шаги)
- [ ] ML Kit: on-device детекция препятствий
- [ ] Виброшаблоны: влево/вправо/опасность
- [ ] Порог частоты команд (не чаще 1/с)
- [ ] Обработка расстояний (м) и углов (°)
- [ ] Локализация (ru/az/en)
- [ ] Настройки приватности (без записи фото/видео, только локальный анализ)

StrictGuideAI_public/
│
├── .git/
│
├── .github/
│   └── workflows/
│        ├── android-ci.yml
│        ├── dump-for-ai.yml
│        └── review.yml
│
├── .gradle/
├── .idea/
│
├── app/
│   ├── build/
│   ├── src/
│   │   └── main/
│   │        ├── java/
│   │        │   └── com/
│   │        │       └── ceyhun/
│   │        │           └── strictguide/
│   │        │               └── MainActivity.kt
│   │        │
│   │        ├── res/
│   │        │   ├── layout/
│   │        │   │    └── activity_main.xml
│   │        │   │
│   │        │   └── values/
│   │        │        ├── colors.xml
│   │        │        ├── strings.xml
│   │        │        └── themes.xml
│   │        │
│   │        ├── AndroidManifest.xml
│   │        └── ic_launcher-playstore.png
│   │
│   ├── build.gradle
│   └── proguard-rules.pro
│
├── build/
│   └── reports/
│        └── problems/
│             └── problems-report.html
│
├── gradle/
│   └── wrapper/
│        ├── gradle-wrapper.jar
│        └── gradle-wrapper.properties
│
├── .env
├── .gitignore
├── ai_bridge_output.txt
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
├── main
├── README_project.md
├── README.md
└── settings.gradle

