#!/bin/bash
# ============================================================
# 校园平台 - MySQL 数据库自动备份脚本
# 策略：保留最近 7 天日备份 + 最近 4 周周备份
# 用法：docker exec campus-backup /backup.sh
# ============================================================

set -euo pipefail

BACKUP_DIR="/backups"
DB_NAME="${DB_NAME:-campus1}"
DB_USER="${DB_USER:-root}"
DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DAY_OF_WEEK=$(date +%u)  # 1=Monday, 7=Sunday

# 确保备份目录存在
mkdir -p "$BACKUP_DIR/daily" "$BACKUP_DIR/weekly"

echo "[$(date)] 开始备份数据库: $DB_NAME"

# 执行 mysqldump
BACKUP_FILE="$BACKUP_DIR/daily/${DB_NAME}_${TIMESTAMP}.sql.gz"
mysqldump \
  -h "$DB_HOST" \
  -P "$DB_PORT" \
  -u "$DB_USER" \
  -p"$DB_PASSWORD" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  "$DB_NAME" | gzip > "$BACKUP_FILE"

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date)] 备份完成: $BACKUP_FILE (大小: $BACKUP_SIZE)"

# 周日额外保存一份周备份
if [ "$DAY_OF_WEEK" -eq 7 ]; then
  WEEKLY_FILE="$BACKUP_DIR/weekly/${DB_NAME}_week$(date +%Y%W).sql.gz"
  cp "$BACKUP_FILE" "$WEEKLY_FILE"
  echo "[$(date)] 周备份已保存: $WEEKLY_FILE"

  # 清理超过 4 周的周备份
  find "$BACKUP_DIR/weekly" -name "*.sql.gz" -mtime +28 -delete
  echo "[$(date)] 已清理超过 28 天的周备份"
fi

# 清理超过 7 天的日备份
find "$BACKUP_DIR/daily" -name "*.sql.gz" -mtime +7 -delete
echo "[$(date)] 已清理超过 7 天的日备份"

# 列出当前备份
echo "[$(date)] 当前备份列表:"
ls -lh "$BACKUP_DIR/daily/" 2>/dev/null || echo "  (无日备份)"
ls -lh "$BACKUP_DIR/weekly/" 2>/dev/null || echo "  (无周备份)"

echo "[$(date)] 备份任务完成"
