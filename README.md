# SkyBound v2

SkyBound v2 — модульный SkyBlock-плагин для Spigot/Paper 1.16.5+ с островами, экономикой, миссиями, магазином, улучшениями, бустерами, банком, варпами, ролями, престижем и системой addon'ов.

README написан так, чтобы по нему можно было поставить и проверить плагин без знания кода.

## Что умеет плагин

| Система | Что делает |
| --- | --- |
| Острова | Создание, удаление, регенерация, home, sethome, приватность, биомы, border |
| Команда острова | Инвайты, роли, kick/ban/unban, trust, coop, передача владельца |
| GUI | Главное меню, магазин, миссии, банк, апгрейды, бустеры, настройки, участники |
| Экономика | Vault-экономика, банк острова, лимиты банка, покупки из банка/кошелька |
| Магазин | Категории, покупка, продажа, предметы с meta/NBT, глобальный баланс цен |
| Миссии | 100+ заданий: добыча, ферма, мобы, крафт, рыбалка, банк, магазин, XP, addon-события |
| Улучшения | Размер острова, команда, банк, генератор, воронки, существа, ферма, спавнеры, дроп |
| Бустеры | Ферма, опыт, спавнеры, генератор, полёт, XP острова |
| Престиж | Обмен XP острова на prestige tokens и отдельный prestige-shop |
| Варпы и визиты | Публичные варпы, посещения островов, лайки, отзывы |
| Сезоны | Настраиваемые сезоны и награды |
| Addon API | Сервисы SkyBoundAPI, события Bukkit, CUSTOM-миссии для внешних addon'ов |

## Требования

Минимально:

- Java 8+
- Spigot/Paper 1.16.5+
- Vault
- любой Vault economy plugin, например EssentialsX Economy

Опционально:

- PlaceholderAPI — плейсхолдеры
- WorldEdit или FastAsyncWorldEdit — schematic-вставка
- SopLib — multi-version helpers, если стоит на сервере
- VoidRift — интеграция событий
- FlexAchievements — интеграция достижений
- SkyBound-IslandCore — addon ядер острова

Если опционального плагина нет, SkyBound всё равно запускается. Интеграция просто отключается.

## Как собрать jar

В корне проекта:

```bash
mvn clean package -DskipTests
```

Готовый jar:

```text
skybound-core/target/SkyBound-2.0.0-SNAPSHOT.jar
```

Для быстрой проверки компиляции:

```bash
mvn -q -DskipTests compile
```

## Как установить на сервер

1. Собери jar командой выше.
2. Положи `SkyBound-2.0.0-SNAPSHOT.jar` в папку `plugins/`.
3. Убедись, что в `plugins/` также есть Vault и economy plugin.
4. Запусти сервер.
5. После первого запуска появится папка `plugins/SkyBound/`.
6. Останови сервер.
7. Настрой конфиги в `plugins/SkyBound/`.
8. Запусти сервер снова.

Если всё хорошо, в консоли будет примерно:

```text
[SkyBound] Storage initialized
[SkyBound] Loaded ... missions
[SkyBound] Loaded ... shop categories
[SkyBound] SkyBound Core ... enabled
```

## Быстрый тест после установки

Зайди на сервер и выполни:

```text
/is create
/is
/is home
/is shop
/is missions
/is bank
/is upgrades
/is boosters
/is value
/is top
```

Мини-чеклист:

- остров создаётся;
- `/is` открывает меню;
- магазин покупает и продаёт предметы;
- банк принимает депозит и выдаёт деньги обратно;
- бустер нельзя купить повторно, пока он активен;
- `/is regen` не ломает остров и не оставляет игрока в старой зоне;
- `/is delete` удаляет остров и данные;
- после restart данные острова сохраняются.

## Основные команды игроков

### Остров

| Команда | Для чего |
| --- | --- |
| `/is` | открыть главное меню или создать остров |
| `/is help` | помощь по командам |
| `/is create [type]` | создать остров |
| `/is home` | телепорт домой |
| `/is sethome` | поставить точку дома |
| `/is name <name>` | переименовать остров |
| `/is lock` | открыть/закрыть остров для гостей |
| `/is biome` | открыть выбор биома |
| `/is delete` | удалить остров |
| `/is regen` | пересоздать остров по schematic |
| `/is value` | пересчитать стоимость острова |
| `/is level` | показать уровень острова |

### Команда острова

| Команда | Для чего |
| --- | --- |
| `/is invite <player>` | пригласить игрока |
| `/is accept` | принять приглашение |
| `/is deny` | отклонить приглашение |
| `/is kick <player>` | выгнать участника |
| `/is ban <player>` | забанить игрока на острове |
| `/is unban <player>` | снять бан |
| `/is leave` | выйти из команды |
| `/is promote <player>` | повысить роль |
| `/is demote <player>` | понизить роль |
| `/is transfer <player>` | передать владельца |
| `/is trust <player>` | доверить игрока |
| `/is untrust <player>` | убрать доверие |
| `/is coop <player>` | временный co-op доступ |
| `/is members` | меню участников |
| `/is settings` | настройки острова и ролей |

### Экономика и прогресс

| Команда | Для чего |
| --- | --- |
| `/is shop` | магазин |
| `/is bank` | банк острова |
| `/is upgrades` | улучшения острова |
| `/is boosters` | временные бустеры |
| `/is missions` | миссии |
| `/is prestige` | престиж острова |
| `/is prestigeshop` или `/is pshop` | магазин престижа |
| `/is autosell` | авто-продажа |
| `/is stats` | личная статистика |
| `/is top` | топ островов |
| `/is chest` | общее хранилище острова |
| `/is logs` | журнал действий острова |

### Социальные функции

| Команда | Для чего |
| --- | --- |
| `/is visit <player>` | посетить остров игрока |
| `/is like` | поставить лайк острову |
| `/is review <island> <1-5> [text]` | оставить отзыв |
| `/is reviews [island]` | посмотреть отзывы |
| `/is gift <player>` | отправить предмет в руке |
| `/is gifts` | открыть подарки |
| `/is trade <player>` | торговля с игроком |
| `/is giveisland <player>` | передать остров |
| `/is sell <player> <price>` | предложить продажу острова |
| `/is buy` | принять покупку острова |

### Варпы и shop chest

| Команда | Для чего |
| --- | --- |
| `/is warps` | список варпов |
| `/is warp <name>` | телепорт на варп |
| `/is setwarp <name>` | создать/переименовать варп |
| `/is delwarp <name>` | удалить варп |
| `/is shopchest set <price>` | сделать сундук магазином |
| `/is shopchest remove` | убрать магазин с сундука |
| `/is border` | управление border |

## Админ-команды

Главная команда:

```text
/sbadmin
```

| Команда | Для чего |
| --- | --- |
| `/sbadmin info` | информация о SkyBound |
| `/sbadmin reload` | перезагрузить конфиги |
| `/sbadmin recalculate` | пересчитать лидерборды/стоимость |
| `/sbadmin addons` | показать зарегистрированные addon'ы |
| `/sbadmin tokens give <player> <amount>` | выдать prestige tokens |
| `/sbadmin tokens take <player> <amount>` | забрать prestige tokens |
| `/sbadmin tokens set <player> <amount>` | установить prestige tokens |

Права:

```text
skybound.admin
skybound.admin.bypass
skybound.island.create
skybound.island.home
skybound.island.invite
```

## Главные конфиги

Все дефолтные файлы лежат в:

```text
skybound-core/src/main/resources/
```

После запуска сервера они копируются в:

```text
plugins/SkyBound/
```

| Файл | За что отвечает |
| --- | --- |
| `config.yml` | язык, мир островов, лимиты, экономика, производительность, prestige |
| `shop.yml` | категории магазина, предметы, buy/sell цены, meta/NBT предметы |
| `missions.yml` | миссии, условия, награды, daily/weekly/reset |
| `upgrades.yml` | цены и эффекты улучшений острова |
| `boosters.yml` | длительность, цена и множитель бустеров |
| `generators.yml` | уровни генератора и шанс руд |
| `schematics.yml` | типы островов и schematic-файлы |
| `recipes.yml` | кастомные рецепты |
| `seasons.yml` | сезоны, награды, активность |
| `prestige-shop.yml` | магазин за prestige tokens |
| `lang/lang_ru.yml` | русский язык |
| `lang/lang_en.yml` | английский язык |
| `plugin.yml` | команды, permissions, softdepend |

## Как балансить экономику

Самые важные настройки находятся в `config.yml`.

### Магазин

```yaml
economy:
  shop:
    buy-multiplier: 1.15
    sell-multiplier: 0.85
```

Что это значит:

- `buy-multiplier` умножает все цены покупки из `shop.yml`;
- `sell-multiplier` умножает все цены продажи из `shop.yml`;
- если магазин слишком дешёвый — подними `buy-multiplier`;
- если игроки слишком быстро фармят деньги — снизь `sell-multiplier`.

Безопасное правило:

```text
sell-price должен быть примерно 35-45% от buy-price
```

### Миссии

```yaml
economy:
  missions:
    money-multiplier: 1.0
    island-xp-multiplier: 0.85
```

Что это значит:

- `money-multiplier` меняет деньги за все миссии;
- `island-xp-multiplier` меняет XP острова за все миссии.

Если игроки слишком быстро качают уровень острова — снизь `island-xp-multiplier`.

### Банк, команда, существа

```yaml
island:
  max-team-size: 3
  max-warps: 5
  base-bank-limit: 1000000
  base-entity-limit: 75
  xp-per-level: 250
```

Что это значит:

- `max-team-size` — базовый размер команды без апгрейдов;
- `max-warps` — базовое количество варпов;
- `base-bank-limit` — базовая вместимость банка;
- `base-entity-limit` — базовый лимит живых существ;
- `xp-per-level` — сколько XP нужно на 1 уровень острова.

### Скорость regen/value/biome

```yaml
performance:
  regen-blocks-per-tick: 12000
  value-blocks-per-tick: 8000
  biome-columns-per-tick: 1024
```

Если сервер лагает при regen/value:

- уменьши значения;
- например `regen-blocks-per-tick: 6000`.

Если сервер мощный и regen слишком медленный:

- можно поднять значения;
- например `regen-blocks-per-tick: 16000`.

## Как добавить предмет в магазин

Открой `shop.yml`, выбери категорию и добавь item:

```yaml
categories:
  blocks:
    items:
      my_block:
        display-name: "&aМой блок"
        material: STONE
        buy-price: 10.0
        sell-price: 4.0
        default-amount: 64
        slot: 0
        lore:
          - "&7Описание предмета"
```

Для предметов с meta/NBT можно использовать Bukkit ItemStack в поле `item` вместо простого `material`.

## Как добавить миссию

Пример простой миссии:

```yaml
missions:
  my_mission:
    display-name: "&aМоя миссия"
    category: "custom"
    icon: DIAMOND
    required-level: 1
    prerequisites: []
    repeatable: false
    time-limit-seconds: 0
    conditions:
      mode: AND
      list:
        - type: BREAK_BLOCK
          target: STONE
          amount: 100
    rewards:
      money: 500.0
      island-xp: 50
      items:
        - material: DIAMOND
          amount: 1
      commands: []
    lore:
      - "&7Сломай 100 камня."
```

Доступные типы условий:

```text
BREAK_BLOCK, PLACE_BLOCK, KILL_MOB, CRAFT_ITEM, SMELT_ITEM, BREW_POTION,
ENCHANT_ITEM, FISH, HARVEST, SHEAR, BREED, TAME, ISLAND_LEVEL, ISLAND_VALUE,
BANK_DEPOSIT, SHOP_BUY, SHOP_SELL, GENERATOR_COLLECT, PICKUP_ITEM, EAT,
WALK_DISTANCE, GAIN_XP, CUSTOM
```

`target: ""` означает “любой предмет/моб/событие”.

## Улучшения и бустеры

`upgrades.yml` — постоянные улучшения острова. Деньги списываются из банка острова.

Основные типы:

```text
ISLAND_SIZE, TEAM_SIZE, BANK_CAPACITY, GENERATOR_TIER, HOPPER_LIMIT,
CROP_GROWTH, SPAWNER_RATE, ENTITY_LIMIT, MOB_DROP, CUSTOM
```

`boosters.yml` — временные усиления. Активный бустер нельзя купить второй раз, пока он не закончится.

Основные типы:

```text
FARMING, EXPERIENCE, SPAWNER, GENERATOR, FLIGHT, ISLAND_XP, CUSTOM
```

## Addon-совместимость

SkyBound регистрирует сервисы через `SkyBoundAPI`:

```java
SkyBoundAPI api = SkyBoundAPI.get();

IslandProvider islands = api.getIslandProvider();
EconomyProvider economy = api.getEconomyProvider();
MissionProvider missions = api.getMissionProvider();
UpgradeProvider upgrades = api.getUpgradeProvider();
ShopProvider shop = api.getShopProvider();
BankProvider bank = api.getBankProvider();
BoosterProvider boosters = api.getBoosterProvider();
```

Зарегистрированные сервисы:

- `IslandProvider`
- `EconomyProvider`
- `TeamProvider`
- `MissionProvider`
- `UpgradeProvider`
- `GeneratorProvider`
- `ShopProvider`
- `BankProvider`
- `BoosterProvider`
- `LeaderboardProvider`
- `AddonRegistry`
- `SeasonProvider`
- `TradeProvider`
- `VisitProvider`
- `PrestigeProvider`

Addon может давать прогресс CUSTOM-миссиям:

```java
SkyBoundAPI.get()
    .getMissionProvider()
    .trackAction(player.getUniqueId(), MissionType.CUSTOM, "EVENT_MY_EVENT", 1);
```

В `missions.yml` уже есть addon-миссии:

- `addon_event_participant`
- `voidrift_event_master`

## PlaceholderAPI

Если PlaceholderAPI установлен, SkyBound регистрирует expansion автоматически.

Проверка:

```text
/papi parse <player> %skybound_island_name%
```

Примеры плейсхолдеров зависят от текущей реализации expansion, но в логах при старте должно быть:

```text
PlaceholderAPI expansion registered
```

## Частые проблемы

### Плагин не включился

Проверь:

- стоит ли Java 8+;
- подходит ли версия сервера 1.16.5+;
- есть ли Vault;
- есть ли economy plugin;
- нет ли ошибок YAML в конфигах.

### Магазин покупает, но не продаёт

Проверь в `shop.yml`:

```yaml
sell-price: 0
```

Если `sell-price` равен `0`, предмет нельзя продать.

### Реген острова слишком медленный

Подними:

```yaml
performance:
  regen-blocks-per-tick: 12000
```

Если сервер лагает — наоборот уменьши.

### Бустер купился, но повторно купить нельзя

Это правильно. Повторная покупка активного бустера запрещена, чтобы игрок не терял деньги впустую.

### После изменения конфига ничего не поменялось

Лучший вариант:

1. остановить сервер;
2. изменить конфиг;
3. запустить сервер.

Для части настроек можно использовать:

```text
/sbadmin reload
```

Но для мира, schematic, storage и некоторых тяжёлых систем лучше делать restart.

## Структура проекта

```text
skybound-api/     публичный API для addon'ов
skybound-core/    основной plugin
pom.xml           общий Maven parent
README.md         эта инструкция
```

## Перед релизом

Проверочный минимум:

```bash
mvn clean package -DskipTests
```

На тестовом сервере:

```text
/is create
/is shop
/is missions
/is bank
/is upgrades
/is boosters
/is regen
/is delete
/sbadmin info
/sbadmin addons
```

Если всё это работает без ошибок в консоли — сборку можно отдавать на тест игрокам.
