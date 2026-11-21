# FerrekKordy – mod Fabric dla Minecraft 1.21.8

Pokazuje koordynaty i od razu przelicza je Overworld ↔ Nether (÷8 / ×8).
Waypoint dodany w jednym wymiarze od razu ma swój odpowiednik w drugim.

## Co robi
- **HUD** (lewy górny róg): Twoje koordy w aktualnym wymiarze + przeliczone na drugi wymiar, kierunek patrzenia, 5 najbliższych waypointów (koordy, odległość, strzałka kierunku).
- **Waypointy**: dodane w Overworldzie pokazują się w Netherze na X/8, Z/8 (i odwrotnie: z Netheru do Overworldu ×8). Wysokość Y w Netherze jest ograniczona do 1–125, żeby nie trafić nad dach z bedrocka.
- **Słupy w świecie**: każdy waypoint ma świecący słup jak z beacona (w swoim kolorze) z nazwą i odległością widoczną przez ściany. Waypointy dalej niż zasięg renderowania pokazują się na horyzoncie, w dobrym kierunku.
- Każdy świat w singlu i każdy serwer ma osobną listę: `.minecraft/config/ferrekkordy/`.

## Klawisze (można zmienić w Opcje → Sterowanie → FerrekKordy)
| Klawisz | Co robi |
|---|---|
| **B** | dodaj waypoint w miejscu, gdzie stoisz (wpisujesz nazwę, Enter zapisuje) |
| **N** | lista waypointów (ukryj/pokaż, usuń) |
| **H** | pokaż/ukryj HUD |
| **K** | pokaż/ukryj słupy waypointów |

## Komendy
- `/wp add <nazwa>` – waypoint tutaj
- `/wp at <x> <y> <z> <nazwa>` – waypoint na podanych koordach (w wymiarze, w którym jesteś)
- `/wp del <nazwa>` – usuń (Tab podpowiada nazwy)
- `/wp list` – lista w czacie
- `/wp gui` – okno z listą
- `/wp calc <x> <z>` – kalkulator Overworld ↔ Nether
- `/wp hud` – pokaż/ukryj HUD
- `/wp beams` – pokaż/ukryj słupy

## Jak zbudować plik .jar
Potrzebujesz **JDK 21** (np. Eclipse Temurin 21 z adoptium.net).

**Windows** – w folderze projektu otwórz terminal i wpisz:
```
gradlew.bat build
```
**Linux/macOS:**
```
./gradlew build
```
Za pierwszym razem pobiera Minecrafta i Fabric, więc może to potrwać kilka minut.
Gotowy mod: `build/libs/ferrekkordy-1.0.0.jar`. Plik `-sources.jar` nie jest potrzebny.

Możesz też otworzyć folder w **IntelliJ IDEA** (jako projekt Gradle) i uruchomić
`Gradle → Tasks → build → build` albo `runClient`, żeby od razu odpalić grę z modem.

## Instalacja
Wrzuć `ferrekkordy-1.0.0.jar` oraz **Fabric API** (wersja dla 1.21.8) do folderu `.minecraft/mods`
i uruchom profil Fabric 1.21.8.
