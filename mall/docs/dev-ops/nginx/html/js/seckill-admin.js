/**
 * 秒杀库存预热管理页面逻辑
 */

let activitiesData = [];

// 页面加载时拉取活动列表
window.addEventListener('DOMContentLoaded', function () {
    loadActivities();
});

async function loadActivities() {
    const tbody = document.getElementById('activitiesTableBody');
    tbody.innerHTML = '<tr class="loading-row"><td colspan="7">加载中...</td></tr>';

    try {
        const url = AppApi.seckill(AppApiPaths.seckill.admin.queryActivities);
        const resp = await fetch(url, { method: 'GET' });
        const json = await resp.json();

        if (json.code !== '0000') {
            tbody.innerHTML = `<tr class="empty-row"><td colspan="7">加载失败：${json.info}</td></tr>`;
            return;
        }

        activitiesData = (json.data && json.data.activities) || [];
        renderActivitiesTable(activitiesData);
        renderBatchSelect(activitiesData);

    } catch (e) {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="7">请求异常：${e.message}</td></tr>`;
    }
}

function renderActivitiesTable(activities) {
    const tbody = document.getElementById('activitiesTableBody');

    if (!activities || activities.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="7">暂无有效秒杀活动</td></tr>';
        return;
    }

    let html = '';
    activities.forEach(function (activity) {
        // 活动行
        html += `<tr class="activity-row">
            <td colspan="3">活动：${escHtml(activity.activityName)}（ID: ${activity.activityId}）</td>
            <td colspan="4" style="color:#64748b;font-weight:400">数据库库存：${activity.remainCount ?? '-'}</td>
        </tr>`;

        // 商品行
        if (!activity.goodsList || activity.goodsList.length === 0) {
            html += `<tr><td colspan="7" class="indent text-muted">该活动下暂无商品</td></tr>`;
        } else {
            activity.goodsList.forEach(function (goods) {
                const isPreheated = goods.currentStock !== null && goods.currentStock !== undefined;
                const statusBadge = isPreheated
                    ? `<span class="badge badge-preheated">已预热 (${goods.currentStock})</span>`
                    : `<span class="badge badge-not-preheated">未预热</span>`;

                html += `<tr>
                    <td class="indent">${escHtml(goods.goodsName)} <span style="color:#94a3b8;font-size:11px">${goods.goodsId}</span></td>
                    <td>${activity.remainCount ?? '-'}</td>
                    <td>${goods.currentStock ?? '<span class="text-muted">-</span>'}</td>
                    <td>${statusBadge}</td>
                    <td><input class="stock-input" type="number" id="stock_${activity.activityId}_${goods.goodsId}" placeholder="默认DB值" min="0"></td>
                    <td><input class="expire-input" type="number" id="expire_${activity.activityId}_${goods.goodsId}" value="120" min="1" max="14400"></td>
                    <td>
                        <button class="btn btn-sm btn-outline"
                            onclick="preheatSingle(${activity.activityId}, '${escHtml(goods.goodsId)}')">
                            预热
                        </button>
                    </td>
                </tr>`;
            });
        }
    });

    tbody.innerHTML = html;
}

function renderBatchSelect(activities) {
    const select = document.getElementById('batchActivitySelect');
    if (!activities || activities.length === 0) {
        select.innerHTML = '<option value="">暂无有效活动</option>';
        return;
    }
    let options = '<option value="">请选择活动</option>';
    activities.forEach(function (a) {
        options += `<option value="${a.activityId}">${escHtml(a.activityName)}（ID: ${a.activityId}）</option>`;
    });
    select.innerHTML = options;
}

async function preheatSingle(activityId, goodsId) {
    const stockInput = document.getElementById(`stock_${activityId}_${goodsId}`);
    const expireInput = document.getElementById(`expire_${activityId}_${goodsId}`);

    const stock = stockInput && stockInput.value ? parseInt(stockInput.value) : null;
    const expireMinutes = expireInput && expireInput.value ? parseInt(expireInput.value) : 120;
    const expireSeconds = expireMinutes * 60;

    await doPreheat(activityId, goodsId, stock, expireSeconds, null);
}

async function batchPreheat() {
    const activityId = document.getElementById('batchActivitySelect').value;
    if (!activityId) {
        showResult('batchResult', 'error', '请先选择活动');
        return;
    }
    const expireMinutes = parseInt(document.getElementById('batchExpireMinutes').value) || 120;
    const expireSeconds = expireMinutes * 60;

    const btn = document.getElementById('batchPreheatBtn');
    btn.disabled = true;
    btn.textContent = '预热中...';

    await doPreheat(parseInt(activityId), 'all', null, expireSeconds, 'batchResult');

    btn.disabled = false;
    btn.textContent = '预热该活动所有商品';
}

async function doPreheat(activityId, goodsId, stock, expireSeconds, resultDivId) {
    try {
        const url = AppApi.seckill(AppApiPaths.seckill.admin.preheat);
        const body = { activityId, goodsId, expireSeconds };
        if (stock !== null) body.stock = stock;

        const resp = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const json = await resp.json();

        if (json.code === '0000') {
            const d = json.data;
            let msg = `预热成功 ${d.successCount} 个商品`;
            if (d.failList && d.failList.length > 0) {
                msg += `，失败 ${d.failList.length} 个：${d.failList.join('；')}`;
                if (resultDivId) showResult(resultDivId, 'error', msg);
                else alert(msg);
            } else {
                if (resultDivId) showResult(resultDivId, 'success', msg);
                else alert(msg);
            }
        } else {
            const errMsg = `预热失败：${json.info}`;
            if (resultDivId) showResult(resultDivId, 'error', errMsg);
            else alert(errMsg);
        }

        // 刷新列表以更新预热状态
        await loadActivities();

    } catch (e) {
        const errMsg = `请求异常：${e.message}`;
        if (resultDivId) showResult(resultDivId, 'error', errMsg);
        else alert(errMsg);
    }
}

function showResult(divId, type, msg) {
    const div = document.getElementById(divId);
    if (!div) return;
    div.className = `result-box ${type}`;
    div.textContent = msg;
    div.style.display = 'block';
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
