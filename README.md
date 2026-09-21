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
- SopLib
- Vault
- любой Vault economy plugin, например EssentialsX Economy

Опционально:

- PlaceholderAPI — плейсхолдеры
- WorldEdit или FastAsyncWorldEdit — schematic-вставка
- VoidRift — интеграция событий
- FlexAchievements — интеграция достижений
- SkyBound-IslandCore — addon ядер острова

SopLib обязателен: он используется как ядро совместимости версий. Если SopLib не установлен, Bukkit/Paper не должен запускать SkyBound.

Если другого опционального плагина нет, SkyBound всё равно запускается. Интеграция просто отключается.

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
3. Убедись, что в `plugins/` также есть SopLib, Vault и economy plugin.
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
| `plugin.yml` | команды, permissions, depend и softdepend |

## Все конфиги подробно

Ниже — что именно можно настраивать в каждом файле. Почти все изменения безопаснее применять так: остановил сервер → изменил файл → запустил сервер. `/sbadmin reload` можно использовать для быстрой проверки, но для мира, schematic, storage, plugin.yml и зависимостей лучше делать полный restart.

### `config.yml`

Главный конфиг ядра.

Основные блоки:

| Блок | Что делает |
| --- | --- |
| `language` | язык сообщений: `ru` → `lang/lang_ru.yml`, `en` → `lang/lang_en.yml` |
| `autosave-seconds` | как часто сохранять данные островов/миссий/апгрейдов |
| `storage` | режим хранения данных, сейчас основной режим — `yaml` |
| `island-world` | имя мира островов, расстояние между островами, радиус, высота, Nether/End |
| `island` | базовые лимиты команды, варпов, банка, существ и XP на уровень |
| `performance` | скорость тяжёлых операций: regen, value scan, смена биома |
| `generator` | включение генератора руд |
| `block-values` | стоимость блоков для `/is value` и топа островов |
| `xp` | настройки пассивного XP |
| `island-core` | что отключать в ядре, если установлен addon IslandCore |
| `economy` | валюта, множители цен магазина и наград миссий |
| `prestige` | уровень/цена/награда престижа |

Самые важные параметры для баланса:

```yaml
island:
  max-team-size: 3
  max-warps: 5
  base-bank-limit: 1000000
  base-entity-limit: 75
  xp-per-level: 250

economy:
  shop:
    buy-multiplier: 1.15
    sell-multiplier: 0.85
  missions:
    money-multiplier: 1.0
    island-xp-multiplier: 0.85

performance:
  regen-blocks-per-tick: 12000
```

Когда менять:

- медленный regen → `performance.regen-blocks-per-tick`;
- слишком быстрые уровни → `island.xp-per-level` или `economy.missions.island-xp-multiplier`;
- слишком много мобов → `island.base-entity-limit` и `upgrades.yml/entity_limit`;
- слишком лёгкая экономика → `economy.shop.*` и `economy.missions.*`.

### `shop.yml`

Файл магазина. Внутри:

```yaml
categories:
  blocks:
    display-name: "..."
    icon: GRASS_BLOCK
    slot: 10
    items:
      cobblestone:
        display-name: "..."
        material: COBBLESTONE
        buy-price: 3.0
        sell-price: 1.0
        default-amount: 64
        slot: 0
        lore: []
```

Категории по умолчанию:

- `blocks` — строительные блоки;
- `ores` — руды, слитки, минералы;
- `farming` — ферма и семена;
- `food` — еда;
- `tools` — инструменты и оружие;
- `armor` — броня;
- `redstone` — редстоун;
- `decoration` — декор;
- `brewing` — зелья/алхимия;
- `spawners` — spawn eggs;
- `special` — редкие предметы.

Поля предмета:

| Поле | Что значит |
| --- | --- |
| `display-name` | название в GUI |
| `material` | Bukkit Material |
| `item` | сериализованный Bukkit ItemStack для meta/NBT предметов |
| `buy-price` | цена покупки, `0` = нельзя купить |
| `sell-price` | цена продажи, `0` = нельзя продать |
| `default-amount` | количество за клик |
| `slot` | слот в меню категории |
| `lore` | описание |
| `commands` | команды от консоли после покупки |

Важно: итоговая цена ещё умножается на `economy.shop.buy-multiplier` и `economy.shop.sell-multiplier` из `config.yml`.

### `missions.yml`

Файл миссий. Сейчас там 100+ миссий и покрыты все основные типы условий.

Структура:

```yaml
missions:
  mission_id:
    display-name: "&aНазвание"
    category: "mining"
    icon: DIAMOND
    required-level: 1
    prerequisites: []
    repeatable: false
    cooldown-seconds: 0
    time-limit-seconds: 0
    reset: daily
    conditions:
      mode: AND
      list:
        - type: BREAK_BLOCK
          target: STONE
          amount: 100
    rewards:
      money: 500.0
      island-xp: 50
      items: []
      commands: []
    lore: []
```

Важные поля:

| Поле | Что значит |
| --- | --- |
| `required-level` | минимальный уровень острова |
| `prerequisites` | какие миссии нужно пройти до этой |
| `repeatable` | можно ли повторять |
| `cooldown-seconds` | задержка повтора |
| `time-limit-seconds` | лимит времени на выполнение |
| `reset` | `daily` или `weekly` для регулярных миссий |
| `conditions.mode` | `AND` — все условия, `OR` — любое |
| `rewards.money` | деньги игроку |
| `rewards.island-xp` | XP острову |
| `rewards.items` | предметы игроку |
| `rewards.commands` | консольные команды |

Типы условий:

```text
BREAK_BLOCK, PLACE_BLOCK, KILL_MOB, CRAFT_ITEM, SMELT_ITEM, BREW_POTION,
ENCHANT_ITEM, FISH, HARVEST, SHEAR, BREED, TAME, ISLAND_LEVEL, ISLAND_VALUE,
BANK_DEPOSIT, SHOP_BUY, SHOP_SELL, GENERATOR_COLLECT, PICKUP_ITEM, EAT,
WALK_DISTANCE, GAIN_XP, CUSTOM
```

`CUSTOM` нужен addon'ам. Например VoidRift/addon может отправить прогресс события в миссию.

### `upgrades.yml`

Файл постоянных улучшений острова. Покупаются из банка острова.

Текущие улучшения:

| ID | Тип | Что улучшает |
| --- | --- | --- |
| `island_size` | `ISLAND_SIZE` | радиус острова |
| `team_size` | `TEAM_SIZE` | лимит участников |
| `bank_capacity` | `BANK_CAPACITY` | вместимость банка |
| `generator_tier` | `GENERATOR_TIER` | уровень генератора |
| `hopper_limit` | `HOPPER_LIMIT` | лимит воронок |
| `crop_growth` | `CROP_GROWTH` | рост растений |
| `spawner_rate` | `SPAWNER_RATE` | скорость спавнеров |
| `entity_limit` | `ENTITY_LIMIT` | лимит существ |
| `mob_drop` | `MOB_DROP` | множитель дропа |

Структура:

```yaml
upgrades:
  island_size:
    display-name: "&aРазмер острова"
    icon: GRASS_BLOCK
    type: ISLAND_SIZE
    levels:
      1: { cost: 15000, value: 10 }
```

`cost` — цена уровня, `value` — эффект уровня.

### `boosters.yml`

Файл временных бустеров. Покупаются из банка острова. Повторная покупка активного бустера запрещена, чтобы игрок не терял деньги.

Текущие бустеры:

- `farming`;
- `experience`;
- `spawner`;
- `generator`;
- `flight`;
- `island_xp`.

Структура:

```yaml
boosters:
  farming:
    display-name: "&aБустер фермерства"
    description: "&7Описание"
    icon: WHEAT
    type: FARMING
    duration-seconds: 900
    cost: 12000.0
    multiplier: 1.75
```

### `generators.yml`

Файл уровней генератора руд.

Текущие tiers:

- `basic`;
- `advanced`;
- `elite`;
- `legendary`.

Структура:

```yaml
tiers:
  basic:
    display-name: "&7Basic Generator"
    icon: COBBLESTONE
    required-level: 1
    distribution:
      COBBLESTONE: 70
      COAL_ORE: 20
      IRON_ORE: 10
```

`distribution` — это веса, не проценты. Чем больше число, тем чаще материал выпадает.

### `schematics.yml`

Файл типов островов и schematic-файлов.

Текущие типы:

- `desert`;
- `desert_nether`;
- `desert_end`;
- `jungle`;
- `jungle_nether`;
- `jungle_end`;
- `mushroom`;
- `mushroom_nether`;
- `mushroom_end`.

Schematic-файлы лежат в:

```text
skybound-core/src/main/resources/schematics/
```

На сервере они должны быть в папке plugin data после первого запуска.

Обычно меняют:

- `display-name`;
- `file`;
- `spawn-offset`;
- `icon`;
- доступность по уровню/измерению, если это задано в конкретной схеме.

### `recipes.yml`

Файл кастомных рецептов.

Текущие рецепты:

- `island_key`;
- `compressed_cobble`;
- `island_compass`.

Обычно структура такая:

```yaml
recipes:
  compressed_cobble:
    type: SHAPED
    result:
      material: COBBLESTONE
      amount: 1
    shape:
      - "CCC"
      - "CCC"
      - "CCC"
    ingredients:
      C: COBBLESTONE
```

После изменения рецептов лучше делать restart.

### `seasons.yml`

Файл сезонов.

Основные поля:

| Поле | Что значит |
| --- | --- |
| `seasons.enabled` | включить/выключить сезоны |
| `seasons.duration-days` | длительность сезона |
| `seasons.auto-reset-islands` | сбрасывать ли острова автоматически |
| `seasons.rewards` | награды за места |
| `seasons.announce-end` | объявлять конец сезона |

Если сезонная система не нужна — поставь:

```yaml
seasons:
  enabled: false
```

### `prestige-shop.yml`

Файл магазина за prestige tokens.

Структура предмета:

```yaml
items:
  reward_id:
    material: NETHERITE_INGOT
    name: "&8Награда"
    lore: []
    cost: 3
    rewards:
      - material: NETHERITE_INGOT
        amount: 4
    commands: []
```

Поддерживается:

- обычные предметы;
- enchantments;
- spawner type;
- firework flight;
- команды от консоли через `{player}`.

### `lang/lang_ru.yml` и `lang/lang_en.yml`

Файлы сообщений.

Здесь меняется весь текст:

- сообщения команд;
- GUI;
- ошибки;
- подсказки;
- подтверждения;
- help-страницы.

Цвета пишутся через `&`, например:

```yaml
island.created: "&aОстров создан!"
```

Если добавляешь новый ключ в один язык, желательно добавить его и во второй.

### `plugin.yml`

Служебный файл Bukkit/Paper.

Что в нём:

- `main` — главный Java-класс;
- `api-version`;
- `depend: [SopLib]` — обязательная зависимость;
- `softdepend` — опциональные интеграции;
- `commands` — регистрация `/is` и `/sbadmin`;
- `permissions` — базовые Bukkit permissions.

Обычно этот файл не меняют на готовом сервере. После изменения нужен restart.

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
- есть ли SopLib;
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
