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
$DB_HOST     = "127.0.0.1"
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

TRUNCATE TABLE videos CASCADE;
TRUNCATE TABLE tiles CASCADE;
TRUNCATE TABLE users CASCADE;
SELECT 'Reset complete - all data cleared.' AS status;
"@

$SQL_SEED = @'
INSERT INTO users (username, email, password, role, active, activation_token, name, surname, address)
VALUES (
    'admin',
    'admin@email.com',
    '$2a$10$D1HCbkFz3yQrEkHOcfuG4e5lFDpq675jdi/Z9yx5n4taX9mlkS9m2',
    'ADMIN',
    true,
    null, null, null, null
);

INSERT INTO videos (title, video_description, video_path, thumbnail_path, video_file_size, upload_duration_ms, location, likes_count, comments_count, views_count, deleted, created_at, updated_at, latitude, longitude, tags)
VALUES
(
    'Amazing Nature Documentary',
    'Explore the breathtaking beauty of nature in this stunning documentary. From majestic mountains to serene oceans, witness the wonders of our planet.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    102400000, 5000, 'Yellowstone National Park, Wyoming',
    1250, 89, 5432, false, NOW(), NOW(), NULL, NULL,
    ARRAY['nature','documentary','wildlife','travel']
),
(
    'Cooking Masterclass: Italian Pasta',
    'Learn to make authentic Italian pasta from scratch. Chef Marco shares his family''s secret recipe passed down through generations.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    85000000, 3500, 'Rome, Italy',
    892, 156, 3210, false, NOW(), NOW(), NULL, NULL,
    ARRAY['cooking','italian','pasta','recipe']
),
(
    'Tech Review: Latest Smartphone',
    'In-depth review of the latest flagship smartphone. We test the camera, battery life, performance, and more in this comprehensive review.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    120000000, 4200, 'San Francisco, CA',
    2103, 234, 8765, false, NOW(), NOW(), NULL, NULL,
    ARRAY['technology','smartphone','review','tech']
),
(
    'Yoga for Beginners',
    'Start your yoga journey with this beginner-friendly session. Perfect for those new to yoga or looking to refresh their practice.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    95000000, 2800, 'Bali, Indonesia',
    567, 78, 2341, false, NOW(), NOW(), NULL, NULL,
    ARRAY['yoga','fitness','wellness','beginners']
),
(
    'Urban Exploration: Hidden City Gems',
    'Join us as we explore hidden gems in the city. From secret cafes to underground art scenes, discover the urban landscape like never before.',
    './uploads/videos/sample.mp4', './uploads/thumbnails/sample.png',
    110000000, 4500, 'New York City, NY',
    1834, 198, 6543, false, NOW(), NOW(), NULL, NULL,
    ARRAY['urban','exploration','travel','city']
);

SELECT 'Seed complete - ' || COUNT(*) || ' video posts inserted.' AS status FROM videos;
'@

# ============================================
# HELPERS
# ============================================
function Run-Sql($sql, $description) {
  Write-Host ""
  Write-Host ">>> $description..." -ForegroundColor Cyan

  
  $result = docker exec jutjubic-postgres-1 psql -U jutjubic_user -d jutjubic_db -c "$sql" 2>&1

  
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
      Write-Host "HINT: Tables do not exist yet " -ForegroundColor Yellow
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