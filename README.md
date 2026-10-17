# Sahayak AI 🤖

**Sahayak AI** — aapka apna Personal AI Agent Android app, Google Gemini se powered. Voice + text input, chat UI, aur basic phone tasks — sab aapke branding me, bina kisi aur AI company ke naam ke.

> "Sahayak" = Hindi me "helper". Yeh app aapki madad ke liye hai.

## ✨ Features (Version 1)

| Feature | Offline? | Detail |
|---|---|---|
| Chat UI (user/AI bubbles) | — | Light + dark theme, Material Design |
| Text input + send | ✅ | Hamesha |
| Voice input (mic) | ❌ Internet chahiye | Android SpeechRecognizer |
| TTS (bol ke jawab dena) | ✅ | Android TextToSpeech (device engine) |
| `hello` / `namaste` | ✅ | Greeting |
| `time` | ✅ | Current time |
| `open <app>` | ✅ | Installed app kholo |
| `search <query>` | ✅ | Google search browser me |
| `call <number>` | ✅ | Confirmation ke baad dialer khulta hai |
| `message <number> saying <text>` | ✅ | Confirmation ke baad messaging app me draft |
| `stop` | ✅ | TTS/mic cancel |
| Koi bhi aur baat | ❌ Gemini se | API key zaroori |

**Important:** App kabhi khud SMS nahi bhejta ya call nahi karta — dialer/messaging app khulta hai aur **aap khud** Send/Call dabate ho. Yeh Android ka safe, official tareeka hai.

## 🔐 Gemini API key setup (ek baar, 2 minute)

1. Phone browser me kholo: **https://aistudio.google.com/apikey**
2. Google account se login → **"Create API key"** dabao.
3. Key copy karo (`AIza...` se shuru).
4. App me: **⚙ Settings → key paste karo → Save**.
5. **"Connection Test karo"** button dabao — "Connection OK!" aana chahiye.

### Security notes (skip mat karo)

- Key **kabhi kisi ko mat dena** — na GitHub, na chat, na screenshot.
- Yeh app key **phone par** (SharedPreferences) save karti hai. **Ye temporary, less-secure prototype hai** — rooted phone par padha ja sakta hai.
- APK me hardcode karne se bhi yahi risk — isliye humne hardcode nahi kiya.
- **Phase 2 (planned):** Cloudflare Workers free-tier backend — key server par rahegi. `.env.example` template ready hai.
- **Free tier limits:** Gemini free tier me per-minute/per-day limits hain (429 error aayega) — thodi der ruk ke try karo. Limits Google badal sakta hai.

## 🛠 Architecture

```
app/src/main/java/com/sahayak/ai/
├── MainActivity.kt        # Chat screen, voice, status
├── SettingsActivity.kt    # API key, model, test button, toggles
├── ChatAdapter.kt         # Chat bubbles
├── ai/AiProvider.kt       # Interface — provider swap karne ke liye
├── ai/GeminiProvider.kt   # Gemini REST API (OkHttp) + errors
├── commands/CommandRouter.kt  # Offline commands + intents
├── commands/AppLauncher.kt    # Installed apps dhoondhna
├── voice/SpeechController.kt # Speech-to-text
├── voice/TtsController.kt    # Text-to-speech
└── util/Prefs.kt          # Settings storage
```

Naya AI provider add karna ho to sirf `AiProvider` implement karo — baaki app waisi hi chalegi.

## 📱 Phone se build (GitHub Codespaces)

**Free tier:** ~120 core-hours/month (2-core space = ~60 ghante) — beginner ke liye kaafi.

1. Repo kholo (phone browser, **desktop mode ON**).
2. **Code → Codespaces → Create codespace on main**.
3. Setup script khud chalegi (5-10 min) — Android SDK install hoga.
4. Terminal me:
   ```
   gradle assembleDebug
   ```
5. First build 10-20 min (dependencies download).
6. APK: `app/build/outputs/apk/debug/app-debug.apk` → download karo.
7. Phone me install: "Unknown sources" allow karo → install.

**Codespaces secret (Phase 2 ke liye):** GitHub → Settings → Codespaces → Secrets → `GEMINI_API_KEY`. Repo me commit nahi hota.

## 🖥 Dost ke laptop se build (alternative)

1. Repo → **Code → Download ZIP** → extract.
2. **Java 17 JDK** install karo (adoptium.net, free).
3. cmd me project folder jao:
   ```
   gradle assembleDebug
   ```
4. APK: `app\build\outputs\apk\debug\app-debug.apk` → phone me copy.
5. 4GB RAM: first build 20-40 min lag sakta hai, aur kuch band rakhna.

## ✅ Testing status (honest report)

- [x] Kotlin files — review se obvious errors check kiye
- [x] Gradle config — AGP 8.5.2 / Kotlin 1.9.24 / Java 17 — compatible
- [x] Ek bug pakda aur fix kiya (testConnection body-read)
- [ ] **Gradle build abhi run NAHI hua** — first build me dependency errors aa sakte hain
- [ ] **Real phone par test NAHI hua** — aap pehle user ho
- [ ] Gemini flow **aapke key ke baad** test hoga (Test Connection button)

Koi guarantee nahi ki pehli baar sab chalega — Troubleshooting dekho.

## 🔧 Troubleshooting

| Error | Fix |
|---|---|
| `Could not resolve gradle plugin` | Internet check; codespace restart |
| `SDK location not found` | `local.properties` ka `sdk.dir` check — setup script dobara chalao |
| Build hang (4GB laptop) | Browser tabs band; aur kuch mat chalao |
| `compileDebugKotlin` fail | Error me `e:` lines dekho — file + line number milega |
| "API key set nahi hai" | Settings me key daalo, Save dabao |
| 429 error | Free tier limit — 1 min ruk ke try karo |
| Mic nahi chalta | Internet on; Settings→Apps→Sahayak AI→Permissions→Microphone |
| TTS awaaz nahi | Phone Settings→Accessibility→Text-to-speech output check |
| Recognizer busy | 2-3 sec ruk ke dobara mic dabao |

## ⚠️ Honest limitations (Android reality)

- Koi normal Android app **dusre apps ko control nahi kar sakta** — hum sirf intents se apps kholte hain.
- **Background SMS/call restricted hai** — isliye confirmation dialog + manual Send/Call hai.
- Voice recognition ko **internet chahiye**. TTS offline bhi chal sakta hai.
- APK me rakhi key **extract ho sakti hai** — isliye Phase 2 backend planned hai.

## 🗺 Roadmap

- **Phase 1 (abhi):** Chat + voice + local commands + Gemini ✅
- **Phase 2:** Cloudflare Workers secure backend + release APK signing
- **Phase 3:** Memory (notes, reminders), app shortcuts, customization

---

Made with ❤️ in India — **Sahayak AI**, aapka apna AI assistant.
