# ============================================
# reset_databases.ps1
#
# Usage:
#   .\reset_databases.ps1              -> reset + reseed (default)
#   .\reset_databases.ps1 -Reset       -> drop tables only
#   .\reset_databases.ps1 -Seed        -> seed data only
# ============================================

param(
  [switch]$Reset,
  [switch]$Seed
)

# ============================================
# CONFIG
# ============================================
$DB_HOST     = "localhost"
$DB_PORT     = "5432"
$DB_NAME     = "jutjubic_db"
$DB_USER     = "jutjubic_user"
$DB_PASSWORD = "banana"

# Auto-detect psql across installed PostgreSQL versions
$pgExe = $null
$pgBasePath = "C:\Program Files\PostgreSQL"

if (Test-Path $pgBasePath) {
  $latestVersion = Get-ChildItem $pgBasePath |
          Where-Object { $_.PSIsContainer } |
          Sort-Object { [int]$_.Name } -Descending |
          Select-Object -First 1
  if ($latestVersion) {
    $candidate = Join-Path $latestVersion.FullName "bin\psql.exe"
    if (Test-Path $candidate) { $pgExe = $candidate }
  }
}

if (-not $pgExe) {
  Write-Host "ERROR: Could not find psql.exe under $pgBasePath" -ForegroundColor Red
  Write-Host "Please install PostgreSQL or update the path in this script." -ForegroundColor Yellow
  exit 1
}

Write-Host "Using psql: $pgExe" -ForegroundColor DarkGray

# ============================================
# SQL
# ============================================
$SQL_RESET = @"
DROP TABLE IF EXISTS tile_videos CASCADE;
DROP TABLE IF EXISTS video_post_tags CASCADE;
DROP TABLE IF EXISTS video_posts CASCADE;
DROP TABLE IF EXISTS tiles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
SELECT 'Reset complete - all tables dropped.' AS status;
"@

$SQL_SEED = @"
INSERT INTO users (username, email, password, role, active, activation_token, name, surname, address)
VALUES (
    'admin',
    'admin@email.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    true,
    null, null, null, null
);

INSERT INTO video_posts (title, video_description, video_path, thumbnail_path, video_file_size, upload_duration_ms, location, likes_count, comments_count, views_count, deleted, created_at, updated_at, latitude, longitude)
VALUES
(
    'Amazing Nature Documentary',
    'Explore the breathtaking beauty of nature in this stunning documentary. From majestic mountains to serene oceans, witness the wonders of our planet.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    102400000, 5000, 'Yellowstone National Park, Wyoming',
    1250, 89, 5432, false, NOW(), NOW(), NULL, NULL
),
(
    'Cooking Masterclass: Italian Pasta',
    'Learn to make authentic Italian pasta from scratch. Chef Marco shares his family''s secret recipe passed down through generations.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    85000000, 3500, 'Rome, Italy',
    892, 156, 3210, false, NOW(), NOW(), NULL, NULL
),
(
    'Tech Review: Latest Smartphone',
    'In-depth review of the latest flagship smartphone. We test the camera, battery life, performance, and more in this comprehensive review.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    120000000, 4200, 'San Francisco, CA',
    2103, 234, 8765, false, NOW(), NOW(), NULL, NULL
),
(
    'Yoga for Beginners',
    'Start your yoga journey with this beginner-friendly session. Perfect for those new to yoga or looking to refresh their practice.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    95000000, 2800, 'Bali, Indonesia',
    567, 78, 2341, false, NOW(), NOW(), NULL, NULL
),
(
    'Urban Exploration: Hidden City Gems',
    'Join us as we explore hidden gems in the city. From secret cafes to underground art scenes, discover the urban landscape like never before.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    110000000, 4500, 'New York City, NY',
    1834, 198, 6543, false, NOW(), NOW(), NULL, NULL
);

INSERT INTO video_post_tags (video_post_id, tag)
SELECT id, unnest(ARRAY['nature','documentary','wildlife','travel'])
FROM video_posts WHERE title = 'Amazing Nature Documentary';

INSERT INTO video_post_tags (video_post_id, tag)
SELECT id, unnest(ARRAY['cooking','italian','pasta','recipe'])
FROM video_posts WHERE title = 'Cooking Masterclass: Italian Pasta';

INSERT INTO video_post_tags (video_post_id, tag)
SELECT id, unnest(ARRAY['technology','smartphone','review','tech'])
FROM video_posts WHERE title = 'Tech Review: Latest Smartphone';

INSERT INTO video_post_tags (video_post_id, tag)
SELECT id, unnest(ARRAY['yoga','fitness','wellness','beginners'])
FROM video_posts WHERE title = 'Yoga for Beginners';

INSERT INTO video_post_tags (video_post_id, tag)
SELECT id, unnest(ARRAY['urban','exploration','travel','city'])
FROM video_posts WHERE title = 'Urban Exploration: Hidden City Gems';

SELECT 'Seed complete - ' || COUNT(*) || ' video posts inserted.' AS status FROM video_posts;
"@

# ============================================
# HELPERS
# ============================================
function Run-Sql($sql, $description) {
  Write-Host ""
  Write-Host ">>> $description..." -ForegroundColor Cyan

  $env:PGPASSWORD = $DB_PASSWORD

  $result = $sql | & "$pgExe" -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME 2>&1

  Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue

  $hasError = ($LASTEXITCODE -ne 0) -or ($result -match "ERROR:")

  if ($hasError) {
    Write-Host "FAILED:" -ForegroundColor Red
    foreach ($line in $result) {
      if ($line -match "ERROR:") {
        Write-Host "  $line" -ForegroundColor Red
      } else {
        Write-Host "  $line" -ForegroundColor Gray
      }
    }
    Write-Host ""
    if ($result -match "relation.*does not exist") {
      Write-Host "HINT: Tables do not exist yet - make sure Spring Boot has started and Hibernate created the tables before seeding." -ForegroundColor Yellow
    }
    Write-Host "!!!!! Operation failed - see errors above !!!!!" -ForegroundColor Red
    exit 1
  }

  Write-Host $result -ForegroundColor Gray
  Write-Host "~~~~ Done ~~~~" -ForegroundColor Green
}

function Prompt-Confirm($message) {
  Write-Host ""
  $response = Read-Host "$message [y/N]"
  return $response -match '^[Yy]$'
}

# ============================================
# MAIN
# ============================================
$doReset = $Reset.IsPresent
$doSeed  = $Seed.IsPresent
$doAll   = -not $doReset -and -not $doSeed

Write-Host "============================================" -ForegroundColor Yellow
Write-Host " Jutjubic DB Manager" -ForegroundColor Yellow
Write-Host " ${DB_NAME} @ ${DB_HOST}:${DB_PORT}" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

if ($doAll) {
  Write-Host " Mode: RESET + SEED" -ForegroundColor Magenta
  if (-not (Prompt-Confirm "This will DROP all tables and reseed. Continue?")) {
    Write-Host "Aborted." -ForegroundColor Yellow; exit 0
  }
  Run-Sql $SQL_RESET "Dropping all tables"
  Write-Host ""
  Write-Host "    Start/Restart your Spring Boot app now so Hibernate recreates the tables." -ForegroundColor Yellow
  if (-not (Prompt-Confirm "Have you started/restarted the app?")) {
    Write-Host "Seed skipped. Run .\reset_databases.ps1 -Seed when ready." -ForegroundColor Yellow; exit 0
  }
  Run-Sql $SQL_SEED "Seeding data"
}
elseif ($doReset) {
  Write-Host " Mode: RESET only" -ForegroundColor Magenta
  if (-not (Prompt-Confirm "This will DROP all tables. Continue?")) {
    Write-Host "Aborted." -ForegroundColor Yellow; exit 0
  }
  Run-Sql $SQL_RESET "Dropping all tables"
  Write-Host "Restart your app, then run .\reset_databases.ps1 -Seed" -ForegroundColor Yellow
}
elseif ($doSeed) {
  Write-Host " Mode: SEED only" -ForegroundColor Magenta
  if (-not (Prompt-Confirm "Insert seed data into existing tables. Continue?")) {
    Write-Host "Aborted." -ForegroundColor Yellow; exit 0
  }
  Run-Sql $SQL_SEED "Seeding data"
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Yellow
Write-Host " ALL OPERATIONS COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Yellow