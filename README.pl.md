# Simply Connect dla Java, Kotlin i Spring Boot

Repozytorium zawiera oficjalnego klienta External API dla Javy 17 oraz starter Spring Boot z opcjonalnym panelem developerskim.

Panel jest domyślnie wyłączony. Po świadomym włączeniu i ustawieniu osobnego hasła pokazuje bramki dostępne dla klucza, wiadomości, flow IVR oraz kolejkę połączeń. Pozwala też wysłać kontrolowany SMS testowy albo dodać połączenie do kolejki. Klucz API zostaje po stronie serwera.

## Instalacja Spring Boot

Dodaj publiczne repozytorium JitPack:

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Następnie dodaj starter — instalacja nie wymaga tokenu GitHub:

```gradle
implementation("com.github.karlos1998.simply-connect-jvm:simply-connect-spring-boot-starter:v0.1.0")
```

Sam klient Java, bez Spring Boot, jest dostępny jako moduł `simply-connect-java` pod tym samym numerem wersji.

Pełna dokumentacja techniczna znajduje się w [README.md](README.md), a instrukcja panelu w [docs/developer-panel.md](docs/developer-panel.md).
