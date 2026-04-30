(function () {
    'use strict';

    const STORAGE_BASE_URL = 'gbm_config_base_url';

    function getBaseUrl() {
        return (document.getElementById('baseUrl') && document.getElementById('baseUrl').value.trim()) || localStorage.getItem(STORAGE_BASE_URL) || 'http://100.86.250.112:8091';
    }

    function setBaseUrl(url) {
        localStorage.setItem(STORAGE_BASE_URL, url);
        if (document.getElementById('baseUrl')) document.getElementById('baseUrl').value = url;
    }

    function api(path, options = {}) {
        const base = getBaseUrl().replace(/\/$/, '');
        const url = base + (path.startsWith('/') ? path : '/api/v1/gbm/config/' + path);
        const opts = {
            headers: { 'Content-Type': 'application/json' },
            ...options
        };
        if (opts.body && typeof opts.body === 'object' && !(opts.body instanceof FormData) && opts.method !== 'GET') {
            opts.body = JSON.stringify(opts.body);
        }
        return fetch(url, opts).then(r => r.json());
    }

    const ConfigAPI = {
        listActivityTypes: () => api('activity_types'),
        getActivityType: (id) => api('activity_type/' + id),
        saveActivityType: (body) => api('activity_type', { method: 'POST', body }),
        updateActivityType: (body) => api('activity_type', { method: 'PUT', body }),
        deleteActivityType: (id) => api('activity_type/' + id, { method: 'DELETE' }),

        listCategories: () => api('categories'),
        getCategory: (id) => api('category/' + id),
        saveCategory: (body) => api('category', { method: 'POST', body }),
        updateCategory: (body) => api('category', { method: 'PUT', body }),
        deleteCategory: (id) => api('category/' + id, { method: 'DELETE' }),
        listSkus: () => api('skus'),
        getSku: (goodsId) => api('sku/' + encodeURIComponent(goodsId)),
        saveSku: (body) => api('sku', { method: 'POST', body }),
        deleteSku: (goodsId) => api('sku/' + encodeURIComponent(goodsId), { method: 'DELETE' }),

        listGroupBuyActivities: () => api('group_buy_activities'),
        getGroupBuyActivity: (activityId) => api('group_buy_activity/' + activityId),
        saveGroupBuyActivity: (body) => api('group_buy_activity', { method: 'POST', body }),
        updateGroupBuyActivityStatus: (activityId, status) => api('group_buy_activity/' + activityId + '/status?status=' + status, { method: 'PUT' }),
        deleteGroupBuyActivity: (activityId) => api('group_buy_activity/' + activityId, { method: 'DELETE' }),

        listSeckillActivities: () => api('seckill_activities'),
        getSeckillActivity: (activityId) => api('seckill_activity/' + activityId),
        saveSeckillActivity: (body) => api('seckill_activity', { method: 'POST', body }),
        updateSeckillActivityStatus: (activityId, status) => api('seckill_activity/' + activityId + '/status?status=' + status, { method: 'PUT' }),
        deleteSeckillActivity: (activityId) => api('seckill_activity/' + activityId, { method: 'DELETE' }),

        listGroupBuyDiscounts: () => api('group_buy_discounts'),
        getGroupBuyDiscount: (discountId) => api('group_buy_discount/' + encodeURIComponent(discountId)),
        saveGroupBuyDiscount: (body) => api('group_buy_discount', { method: 'POST', body }),
        updateGroupBuyDiscount: (body) => api('group_buy_discount', { method: 'PUT', body }),
        deleteGroupBuyDiscount: (discountId) => api('group_buy_discount/' + encodeURIComponent(discountId), { method: 'DELETE' }),

        listSCSkuActivities: (source, channel) => {
            let path = 'sc_sku_activities';
            if (source || channel) path += '?source=' + encodeURIComponent(source || '') + '&channel=' + encodeURIComponent(channel || '');
            return api(path);
        },
        getSCSkuActivity: (id) => api('sc_sku_activity/' + id),
        saveSCSkuActivity: (body) => api('sc_sku_activity', { method: 'POST', body }),
        updateSCSkuActivity: (body) => api('sc_sku_activity', { method: 'PUT', body }),
        deleteSCSkuActivity: (id) => api('sc_sku_activity/' + id, { method: 'DELETE' }),

        listCrowdTags: () => api('crowd_tags'),
        getCrowdTag: (tagId) => api('crowd_tag/' + encodeURIComponent(tagId)),
        saveCrowdTag: (body) => api('crowd_tag', { method: 'POST', body }),
        updateCrowdTagsStatistics: (tagId, statistics) => api('crowd_tags/statistics?tagId=' + encodeURIComponent(tagId) + '&statistics=' + statistics, { method: 'PUT' }),
        deleteCrowdTag: (tagId) => api('crowd_tag/' + encodeURIComponent(tagId), { method: 'DELETE' }),

        updateDcc: (key, value) => fetch(getBaseUrl().replace(/\/$/, '') + '/api/v1/gbm/dcc/update_config?key=' + encodeURIComponent(key) + '&value=' + encodeURIComponent(value)).then(r => r.json())
    };

    function isOk(res) {
        return res && res.code === '0000';
    }

    function renderActivityTypes(list) {
        const tbody = document.getElementById('tbody-activity_types');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.typeName ?? '') + '</td><td>' + (item.typeCode ?? '') + '</td><td>' + (item.status ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-activity-type" data-id="' + (item.id ?? '') + '">编辑</button> <button type="button" class="btn btn-sm btn-danger btn-delete-activity-type" data-id="' + (item.id ?? '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="4">暂无数据</td></tr>';
    }

    function renderCategories(list) {
        const tbody = document.getElementById('tbody-categories');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.name ?? '') + '</td><td>' + (item.code ?? '') + '</td><td>' + (item.iconUrl ?? '') + '</td><td>' + (item.sortOrder ?? '') + '</td><td>' + (item.status ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-category" data-id="' + (item.id ?? '') + '">编辑</button> <button type="button" class="btn btn-sm btn-danger btn-delete-category" data-id="' + (item.id ?? '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="6">暂无数据</td></tr>';
    }

    function renderSkus(list) {
        const tbody = document.getElementById('tbody-skus');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.source ?? '') + '</td><td>' + (item.channel ?? '') + '</td><td>' + (item.goodsId ?? '') + '</td><td>' + (item.goodsName ?? '') + '</td><td>' + _fmtMoney(item.originalPrice) + '</td><td><button type="button" class="btn btn-sm btn-edit" data-goodsId="' + (item.goodsId || '') + '">编辑</button> <button type="button" class="btn btn-sm btn-danger btn-delete-sku" data-goodsId="' + (item.goodsId || '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="6">暂无数据</td></tr>';
    }

    function _fmtDt(d) { return d ? (d + '').slice(0, 19).replace('T', ' ') : ''; }
    function _fmtMoney(v) {
        var n = Number(v);
        return isNaN(n) ? '' : Math.max(0, n).toFixed(2);
    }

    function renderGroupBuyActivities(list) {
        const tbody = document.getElementById('tbody-group_buy_activities');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.activityId ?? '') + '</td><td>' + (item.activityName ?? '') + '</td><td>' + (item.discountId ?? '') + '</td><td>' + (item.groupType ?? '') + '</td><td>' + (item.takeLimitCount ?? '') + '</td><td>' + (item.target ?? '') + '</td><td>' + (item.validTime ?? '') + '</td><td>' + (item.status ?? '') + '</td><td>' + _fmtDt(item.startTime) + '</td><td>' + _fmtDt(item.endTime) + '</td><td>' + (item.tagId ?? '') + '</td><td>' + (item.tagScope ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-activity" data-activityId="' + (item.activityId ?? '') + '">编辑</button> <button type="button" class="btn btn-sm btn-status-activity" data-activityId="' + (item.activityId ?? '') + '">改状态</button> <button type="button" class="btn btn-sm btn-danger btn-delete-activity" data-activityId="' + (item.activityId ?? '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="13">暂无数据</td></tr>';
    }

    function renderSeckillActivities(list) {
        const tbody = document.getElementById('tbody-seckill_activities');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.activityId ?? '') + '</td><td>' + (item.activityName ?? '') + '</td><td>' + (item.goodsId ?? '') + '</td><td>' + _fmtMoney(item.seckillPrice) + '</td><td>' + (item.stockCount ?? item.stock ?? '') + '</td><td>' + (item.status ?? '') + '</td><td>' + _fmtDt(item.startTime) + '</td><td>' + _fmtDt(item.endTime) + '</td><td>' + (item.tagId ?? '') + '</td><td>' + (item.tagScope ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-seckill" data-activityId="' + (item.activityId ?? '') + '">编辑</button> <button type="button" class="btn btn-sm btn-danger btn-delete-seckill" data-activityId="' + (item.activityId ?? '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="11">暂无数据</td></tr>';
    }

    function renderGroupBuyDiscounts(list) {
        const tbody = document.getElementById('tbody-group_buy_discounts');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.discountId ?? '') + '</td><td>' + (item.discountName ?? '') + '</td><td>' + (item.discountDesc ?? '') + '</td><td>' + (item.discountType ?? '') + '</td><td>' + (item.marketPlan ?? '') + '</td><td>' + (item.marketExpr ?? '') + '</td><td>' + (item.tagId ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-discount" data-discountId="' + (item.discountId || '') + '">编辑</button> <button type="button" class="btn btn-sm btn-danger btn-delete-discount" data-discountId="' + (item.discountId || '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="8">暂无数据</td></tr>';
    }

    function renderSCSkuActivities(list) {
        const tbody = document.getElementById('tbody-sc_sku_activities');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.source ?? '') + '</td><td>' + (item.channel ?? '') + '</td><td>' + (item.activityId ?? '') + '</td><td>' + (item.goodsId ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-sc" data-id="' + (item.id ?? '') + '">编辑</button> <button type="button" class="btn btn-sm btn-danger btn-delete-sc" data-id="' + (item.id ?? '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="5">暂无数据</td></tr>';
    }

    function renderCrowdTags(list) {
        const tbody = document.getElementById('tbody-crowd_tags');
        if (!tbody) return;
        tbody.innerHTML = (list || []).map(item =>
            '<tr><td>' + (item.tagId ?? '') + '</td><td>' + (item.tagName ?? '') + '</td><td>' + (item.tagDesc ?? '') + '</td><td>' + (item.statistics ?? '') + '</td><td><button type="button" class="btn btn-sm btn-edit-tag-stat" data-tagId="' + (item.tagId || '') + '" data-statistics="' + (item.statistics ?? '') + '">改统计量</button> <button type="button" class="btn btn-sm btn-danger btn-delete-crowd-tag" data-tagId="' + (item.tagId || '') + '">删除</button></td></tr>'
        ).join('') || '<tr><td colspan="5">暂无数据</td></tr>';
    }

    function loadPanel(panelId) {
        switch (panelId) {
            case 'activity_types':
                ConfigAPI.listActivityTypes().then(res => { if (isOk(res)) renderActivityTypes(res.data); });
                break;
            case 'categories':
                ConfigAPI.listCategories().then(res => { if (isOk(res)) renderCategories(res.data); });
                break;
            case 'skus':
                ConfigAPI.listSkus().then(res => { if (isOk(res)) renderSkus(res.data); });
                break;
            case 'group_buy_activities':
                ConfigAPI.listGroupBuyActivities().then(res => { if (isOk(res)) renderGroupBuyActivities(res.data); });
                break;
            case 'seckill_activities':
                ConfigAPI.listSeckillActivities().then(res => { if (isOk(res)) renderSeckillActivities(res.data); });
                break;
            case 'group_buy_discounts':
                ConfigAPI.listGroupBuyDiscounts().then(res => { if (isOk(res)) renderGroupBuyDiscounts(res.data); });
                break;
            case 'sc_sku_activities':
                var s = document.getElementById('filter-source'), c = document.getElementById('filter-channel');
                ConfigAPI.listSCSkuActivities(s ? s.value : '', c ? c.value : '').then(res => { if (isOk(res)) renderSCSkuActivities(res.data); });
                break;
            case 'crowd_tags':
                ConfigAPI.listCrowdTags().then(res => { if (isOk(res)) renderCrowdTags(res.data); });
                break;
        }
    }

    var currentFormType = null;
    var currentEditId = null;

    function openModal(title, bodyHtml, onSubmit) {
        var modal = document.getElementById('modal');
        var titleEl = document.getElementById('modalTitle');
        var bodyEl = document.getElementById('modalBody');
        if (titleEl) titleEl.textContent = title;
        if (bodyEl) bodyEl.innerHTML = bodyHtml;
        if (modal) modal.classList.add('open');

        var submitBtn = document.getElementById('modalSubmit');
        if (submitBtn) {
            var handler = function () {
                if (onSubmit && typeof onSubmit === 'function') onSubmit();
            };
            submitBtn.replaceWith(submitBtn.cloneNode(true));
            document.getElementById('modalSubmit').addEventListener('click', handler);
        }
    }

    function closeModal() {
        document.getElementById('modal').classList.remove('open');
        currentFormType = null;
        currentEditId = null;
    }

    function formActivityType(editItem) {
        return '<div class="form-grid">' +
            '<label>名称 <input type="text" id="f-typeName" value="' + (editItem ? (editItem.typeName || '') : '') + '"></label>' +
            '<label>编码 <input type="text" id="f-typeCode" value="' + (editItem ? (editItem.typeCode || '') : '') + '" ' + (editItem ? 'readonly' : '') + ' placeholder="如 group_buy"></label>' +
            '<label>状态 <select id="f-status"><option value="0"' + ((editItem && editItem.status === 0) ? ' selected' : '') + '>禁用</option><option value="1"' + ((editItem && editItem.status !== 0) ? ' selected' : '') + '>启用</option></select></label>' +
            (editItem && editItem.id ? '<input type="hidden" id="f-id" value="' + editItem.id + '">' : '') +
            '</div>';
    }

    function formCategory(editItem) {
        return '<div class="form-grid">' +
            '<label>名称 <input type="text" id="f-name" value="' + (editItem ? (editItem.name || '') : '') + '"></label>' +
            '<label>编码 <input type="text" id="f-code" value="' + (editItem ? (editItem.code || '') : '') + '" ' + (editItem ? 'readonly' : '') + '></label>' +
            '<label>图标URL <input type="text" id="f-iconUrl" value="' + (editItem ? (editItem.iconUrl || '') : '') + '"></label>' +
            '<label>排序 <input type="number" id="f-sortOrder" value="' + (editItem ? (editItem.sortOrder ?? 0) : '0') + '"></label>' +
            '<label>状态 <select id="f-status"><option value="0"' + ((editItem && editItem.status === 0) ? ' selected' : '') + '>禁用</option><option value="1"' + ((!editItem || editItem.status !== 0) ? ' selected' : '') + '>启用</option></select></label>' +
            (editItem && editItem.id ? '<input type="hidden" id="f-id" value="' + editItem.id + '">' : '') +
            '</div>';
    }

    function formCrowdTag(editItem) {
        return '<div class="form-grid">' +
            '<label>标签ID <input type="text" id="f-tagId" value="' + (editItem ? (editItem.tagId || '') : '') + '" ' + (editItem ? 'readonly' : '') + ' placeholder="如 RQ_xxx"></label>' +
            '<label>名称 <input type="text" id="f-tagName" value="' + (editItem ? (editItem.tagName || '') : '') + '"></label>' +
            '<label>描述 <input type="text" id="f-tagDesc" value="' + (editItem ? (editItem.tagDesc || '') : '') + '"></label>' +
            '<label>统计量 <input type="number" id="f-statistics" value="' + (editItem ? (editItem.statistics ?? 0) : '0') + '"></label>' +
            '</div>';
    }

    function formSku(editItem) {
        return '<div class="form-grid">' +
            '<label>渠道 source <input type="text" id="f-source" value="' + (editItem ? (editItem.source || 's01') : 's01') + '" placeholder="如 s01"></label>' +
            '<label>来源 channel <input type="text" id="f-channel" value="' + (editItem ? (editItem.channel || 'c01') : 'c01') + '" placeholder="如 c01"></label>' +
            '<label>商品ID goods_id <input type="text" id="f-goodsId" value="' + (editItem ? (editItem.goodsId || '') : '') + '" ' + (editItem ? 'readonly' : '') + ' placeholder="唯一"></label>' +
            '<label>商品名称 goods_name <input type="text" id="f-goodsName" value="' + (editItem ? (editItem.goodsName || '') : '') + '"></label>' +
            '<label>原价 original_price <input type="number" step="0.01" min="0" id="f-originalPrice" value="' + (editItem ? (editItem.originalPrice ?? '') : '') + '"></label>' +
            '</div>';
    }

    function formGroupBuyActivity(editItem) {
        return '<div class="form-grid">' +
            '<label>活动ID <input type="number" id="f-activityId" value="' + (editItem ? (editItem.activityId ?? '') : '') + '" ' + (editItem ? 'readonly' : '') + '></label>' +
            '<label>活动名称 <input type="text" id="f-activityName" value="' + (editItem ? (editItem.activityName || '') : '') + '"></label>' +
            '<label>折扣ID <input type="text" id="f-discountId" value="' + (editItem ? (editItem.discountId || '') : '') + '"></label>' +
            '<label>拼团方式 <select id="f-groupType"><option value="0"' + ((editItem && editItem.groupType === 0) ? ' selected' : '') + '>自动成团</option><option value="1"' + ((editItem && editItem.groupType === 1) ? ' selected' : '') + '>目标成团</option></select></label>' +
            '<label>拼团目标人数 <input type="number" id="f-target" value="' + (editItem ? (editItem.target ?? 3) : '3') + '"></label>' +
            '<label>拼团时长(分钟) <input type="number" id="f-validTime" value="' + (editItem ? (editItem.validTime ?? 15) : '15') + '"></label>' +
            '<label>拼团次数限制 <input type="number" id="f-takeLimitCount" value="' + (editItem ? (editItem.takeLimitCount ?? 1) : '1') + '"></label>' +
            '<label>状态 <select id="f-status"><option value="0"' + ((editItem && editItem.status === 0) ? ' selected' : '') + '>创建</option><option value="1"' + ((editItem && editItem.status === 1) ? ' selected' : '') + '>生效</option><option value="2"' + ((editItem && editItem.status === 2) ? ' selected' : '') + '>过期</option><option value="3"' + ((editItem && editItem.status === 3) ? ' selected' : '') + '>废弃</option></select></label>' +
            '<label>开始时间 <input type="datetime-local" id="f-startTime" value="' + (editItem && editItem.startTime ? editItem.startTime.slice(0, 16) : '') + '"></label>' +
            '<label>结束时间 <input type="datetime-local" id="f-endTime" value="' + (editItem && editItem.endTime ? editItem.endTime.slice(0, 16) : '') + '"></label>' +
            '<label>人群标签tagId <input type="text" id="f-tagId" value="' + (editItem ? (editItem.tagId || '') : '') + '"></label>' +
            '<label>人群范围tagScope <input type="text" id="f-tagScope" value="' + (editItem ? (editItem.tagScope || '') : '') + '"></label>' +
            '</div>';
    }

    function formSeckillActivity(editItem) {
        return '<div class="form-grid">' +
            '<label>活动ID <input type="number" id="f-activityId" value="' + (editItem ? (editItem.activityId ?? '') : '') + '" ' + (editItem ? 'readonly' : '') + '></label>' +
            '<label>活动名称 <input type="text" id="f-activityName" value="' + (editItem ? (editItem.activityName || '') : '') + '"></label>' +
            '<label>商品ID <input type="text" id="f-goodsId" value="' + (editItem ? (editItem.goodsId || '') : '') + '"></label>' +
            '<label>秒杀价 <input type="number" step="0.01" min="0" id="f-seckillPrice" value="' + (editItem ? (editItem.seckillPrice ?? '') : '') + '"></label>' +
            '<label>库存 <input type="number" id="f-stock" value="' + (editItem ? (editItem.stockCount ?? editItem.stock ?? 0) : '0') + '"></label>' +
            '<label>状态 <select id="f-status"><option value="0">创建</option><option value="1">生效</option><option value="2">过期</option><option value="3">废弃</option></select></label>' +
            '<label>开始时间 <input type="datetime-local" id="f-startTime" value="' + (editItem && editItem.startTime ? editItem.startTime.slice(0, 16) : '') + '"></label>' +
            '<label>结束时间 <input type="datetime-local" id="f-endTime" value="' + (editItem && editItem.endTime ? editItem.endTime.slice(0, 16) : '') + '"></label>' +
            '</div>';
    }

    function formGroupBuyDiscount(editItem) {
        return '<div class="form-grid">' +
            '<label>折扣ID <input type="text" id="f-discountId" value="' + (editItem ? (editItem.discountId || '') : '') + '" ' + (editItem ? 'readonly' : '') + ' maxlength="8"></label>' +
            '<label>折扣名称 <input type="text" id="f-discountName" value="' + (editItem ? (editItem.discountName || '') : '') + '"></label>' +
            '<label>折扣描述 <input type="text" id="f-discountDesc" value="' + (editItem ? (editItem.discountDesc || '') : '') + '"></label>' +
            '<label>折扣类型 <select id="f-discountType"><option value="0">base</option><option value="1">tag</option></select></label>' +
            '<label>营销计划 <select id="f-marketPlan"><option value="ZJ"' + ((editItem && editItem.marketPlan === 'ZJ') ? ' selected' : '') + '>ZJ直减</option><option value="MJ">MJ满减</option><option value="N">N元购</option></select></label>' +
            '<label>营销表达式 <input type="text" id="f-marketExpr" value="' + (editItem ? (editItem.marketExpr || '') : '') + '" placeholder="如 20"></label>' +
            '<label>人群标签tagId <input type="text" id="f-tagId" value="' + (editItem ? (editItem.tagId || '') : '') + '"></label>' +
            '</div>';
    }

    function formSCSkuActivity(editItem) {
        return '<div class="form-grid">' +
            (editItem && editItem.id ? '<input type="hidden" id="f-id" value="' + editItem.id + '">' : '') +
            '<label>渠道 source <input type="text" id="f-source" value="' + (editItem ? (editItem.source || 's01') : 's01') + '" ' + (editItem ? 'readonly' : '') + '></label>' +
            '<label>来源 channel <input type="text" id="f-channel" value="' + (editItem ? (editItem.channel || 'c01') : 'c01') + '" ' + (editItem ? 'readonly' : '') + '></label>' +
            '<label>活动ID <input type="number" id="f-activityId" value="' + (editItem ? (editItem.activityId ?? '') : '') + '"></label>' +
            '<label>活动类型 <input type="text" id="f-activityType" value="' + (editItem ? (editItem.activityType || 'group_buy') : 'group_buy') + '" placeholder="group_buy/seckill"></label>' +
            '<label>商品ID <input type="text" id="f-goodsId" value="' + (editItem ? (editItem.goodsId || '') : '') + '"></label>' +
            '</div>';
    }

    function submitActivityType() {
        var idEl = document.getElementById('f-id');
        var typeName = document.getElementById('f-typeName') && document.getElementById('f-typeName').value.trim();
        var typeCode = document.getElementById('f-typeCode') && document.getElementById('f-typeCode').value.trim();
        if (!typeName || !typeCode) { alert('请填写名称、编码'); return; }
        var status = parseInt((document.getElementById('f-status') && document.getElementById('f-status').value) || '1', 10);
        if (idEl && idEl.value) {
            ConfigAPI.updateActivityType({ id: parseInt(idEl.value, 10), typeName: typeName, typeCode: typeCode, status: status }).then(function (res) {
                if (isOk(res)) { alert('更新成功'); closeModal(); loadPanel('activity_types'); } else { alert(res.info || res.code); }
            });
        } else {
            ConfigAPI.saveActivityType({ typeName: typeName, typeCode: typeCode, status: status }).then(function (res) {
                if (isOk(res)) { alert('新增成功'); closeModal(); loadPanel('activity_types'); } else { alert(res.info || res.code); }
            });
        }
    }

    function submitCategory() {
        var idEl = document.getElementById('f-id');
        var name = document.getElementById('f-name') && document.getElementById('f-name').value.trim();
        var code = document.getElementById('f-code') && document.getElementById('f-code').value.trim();
        if (!name || !code) { alert('请填写名称、编码'); return; }
        var sortOrder = parseInt((document.getElementById('f-sortOrder') && document.getElementById('f-sortOrder').value) || '0', 10);
        var status = parseInt((document.getElementById('f-status') && document.getElementById('f-status').value) || '1', 10);
        var iconUrl = (document.getElementById('f-iconUrl') && document.getElementById('f-iconUrl').value) || null;
        if (idEl && idEl.value) {
            ConfigAPI.updateCategory({ id: parseInt(idEl.value, 10), name: name, code: code, iconUrl: iconUrl, sortOrder: sortOrder, status: status }).then(function (res) {
                if (isOk(res)) { alert('更新成功'); closeModal(); loadPanel('categories'); } else { alert(res.info || res.code); }
            });
        } else {
            ConfigAPI.saveCategory({ name: name, code: code, iconUrl: iconUrl, sortOrder: sortOrder, status: status }).then(function (res) {
                if (isOk(res)) { alert('新增成功'); closeModal(); loadPanel('categories'); } else { alert(res.info || res.code); }
            });
        }
    }

    function submitCrowdTag() {
        var tagId = document.getElementById('f-tagId') && document.getElementById('f-tagId').value.trim();
        var tagName = document.getElementById('f-tagName') && document.getElementById('f-tagName').value.trim();
        if (!tagId || !tagName) { alert('请填写标签ID、名称'); return; }
        var tagDesc = (document.getElementById('f-tagDesc') && document.getElementById('f-tagDesc').value) || '';
        var statistics = parseInt((document.getElementById('f-statistics') && document.getElementById('f-statistics').value) || '0', 10);
        ConfigAPI.saveCrowdTag({ tagId: tagId, tagName: tagName, tagDesc: tagDesc, statistics: statistics }).then(function (res) {
            if (isOk(res)) { alert('新增成功'); closeModal(); loadPanel('crowd_tags'); } else { alert(res.info || res.code); }
        });
    }

    function submitSku() {
        var goodsId = document.getElementById('f-goodsId') && document.getElementById('f-goodsId').value.trim();
        var goodsName = document.getElementById('f-goodsName') && document.getElementById('f-goodsName').value.trim();
        var originalPrice = document.getElementById('f-originalPrice') && document.getElementById('f-originalPrice').value;
        var originalPriceNum = parseFloat(originalPrice);
        if (!goodsId || !goodsName || originalPrice === '' || isNaN(originalPriceNum) || originalPriceNum < 0) {
            alert('请填写商品ID、名称和不小于 0 的原价');
            return;
        }
        ConfigAPI.saveSku({
            goodsId: goodsId,
            goodsName: goodsName,
            originalPrice: originalPriceNum,
            source: (document.getElementById('f-source') && document.getElementById('f-source').value) || 's01',
            channel: (document.getElementById('f-channel') && document.getElementById('f-channel').value) || 'c01'
        }).then(function (res) {
            if (isOk(res)) { alert('保存成功'); closeModal(); loadPanel('skus'); } else { alert(res.info || res.code); }
        });
    }

    function submitGroupBuyActivity() {
        var activityId = document.getElementById('f-activityId') && document.getElementById('f-activityId').value;
        var activityName = document.getElementById('f-activityName') && document.getElementById('f-activityName').value.trim();
        var discountId = document.getElementById('f-discountId') && document.getElementById('f-discountId').value.trim();
        if (!activityId || !activityName || !discountId) {
            alert('请填写活动ID、活动名称、折扣ID');
            return;
        }
        var startTime = document.getElementById('f-startTime') && document.getElementById('f-startTime').value;
        var endTime = document.getElementById('f-endTime') && document.getElementById('f-endTime').value;
        if (startTime) startTime = startTime.replace('T', ' ') + ':00';
        if (endTime) endTime = endTime.replace('T', ' ') + ':00';
        ConfigAPI.saveGroupBuyActivity({
            activityId: parseInt(activityId, 10),
            activityName: activityName,
            discountId: discountId,
            groupType: parseInt((document.getElementById('f-groupType') && document.getElementById('f-groupType').value) || '0', 10),
            target: parseInt((document.getElementById('f-target') && document.getElementById('f-target').value) || '3', 10),
            validTime: parseInt((document.getElementById('f-validTime') && document.getElementById('f-validTime').value) || '15', 10),
            takeLimitCount: parseInt((document.getElementById('f-takeLimitCount') && document.getElementById('f-takeLimitCount').value) || '1', 10),
            status: parseInt((document.getElementById('f-status') && document.getElementById('f-status').value) || '0', 10),
            startTime: startTime || null,
            endTime: endTime || null,
            tagId: (document.getElementById('f-tagId') && document.getElementById('f-tagId').value) || null,
            tagScope: (document.getElementById('f-tagScope') && document.getElementById('f-tagScope').value) || null
        }).then(function (res) {
            if (isOk(res)) { alert('保存成功'); closeModal(); loadPanel('group_buy_activities'); } else { alert(res.info || res.code); }
        });
    }

    function submitSeckillActivity() {
        var activityId = document.getElementById('f-activityId') && document.getElementById('f-activityId').value;
        var activityName = document.getElementById('f-activityName') && document.getElementById('f-activityName').value.trim();
        var goodsId = document.getElementById('f-goodsId') && document.getElementById('f-goodsId').value.trim();
        var seckillPrice = document.getElementById('f-seckillPrice') && document.getElementById('f-seckillPrice').value;
        var seckillPriceNum = parseFloat(seckillPrice);
        if (!activityId || !activityName || !goodsId || seckillPrice === '' || isNaN(seckillPriceNum) || seckillPriceNum < 0) {
            alert('请填写活动ID、活动名称、商品ID和不小于 0 的秒杀价');
            return;
        }
        var startTime = document.getElementById('f-startTime') && document.getElementById('f-startTime').value;
        var endTime = document.getElementById('f-endTime') && document.getElementById('f-endTime').value;
        if (startTime) startTime = startTime.replace('T', ' ') + ':00';
        if (endTime) endTime = endTime.replace('T', ' ') + ':00';
        ConfigAPI.saveSeckillActivity({
            activityId: parseInt(activityId, 10),
            activityName: activityName,
            goodsId: goodsId,
            seckillPrice: seckillPriceNum,
            stockCount: parseInt((document.getElementById('f-stock') && document.getElementById('f-stock').value) || '0', 10),
            stock: parseInt((document.getElementById('f-stock') && document.getElementById('f-stock').value) || '0', 10),
            status: parseInt((document.getElementById('f-status') && document.getElementById('f-status').value) || '0', 10),
            startTime: startTime || null,
            endTime: endTime || null
        }).then(function (res) {
            if (isOk(res)) { alert('保存成功'); closeModal(); loadPanel('seckill_activities'); } else { alert(res.info || res.code); }
        });
    }

    function submitGroupBuyDiscount() {
        var discountId = document.getElementById('f-discountId') && document.getElementById('f-discountId').value.trim();
        var discountName = document.getElementById('f-discountName') && document.getElementById('f-discountName').value.trim();
        var marketExpr = document.getElementById('f-marketExpr') && document.getElementById('f-marketExpr').value.trim();
        if (!discountId || !discountName || !marketExpr) {
            alert('请填写折扣ID、名称、营销表达式');
            return;
        }
        var body = {
            discountId: discountId,
            discountName: discountName,
            discountDesc: (document.getElementById('f-discountDesc') && document.getElementById('f-discountDesc').value) || '',
            discountType: parseInt((document.getElementById('f-discountType') && document.getElementById('f-discountType').value) || '0', 10),
            marketPlan: (document.getElementById('f-marketPlan') && document.getElementById('f-marketPlan').value) || 'ZJ',
            marketExpr: marketExpr,
            tagId: (document.getElementById('f-tagId') && document.getElementById('f-tagId').value) || null
        };
        var apiCall = currentEditId ? ConfigAPI.updateGroupBuyDiscount(body) : ConfigAPI.saveGroupBuyDiscount(body);
        apiCall.then(function (res) {
            if (isOk(res)) { alert('保存成功'); closeModal(); loadPanel('group_buy_discounts'); } else { alert(res.info || res.code); }
        });
    }

    function submitSCSkuActivity() {
        var idEl = document.getElementById('f-id');
        var source = (document.getElementById('f-source') && document.getElementById('f-source').value) || 's01';
        var channel = (document.getElementById('f-channel') && document.getElementById('f-channel').value) || 'c01';
        var activityId = document.getElementById('f-activityId') && document.getElementById('f-activityId').value;
        var goodsId = document.getElementById('f-goodsId') && document.getElementById('f-goodsId').value.trim();
        if (!activityId || !goodsId) {
            alert('请填写活动ID、商品ID');
            return;
        }
        var body = {
            source: source,
            channel: channel,
            activityId: parseInt(activityId, 10),
            activityType: (document.getElementById('f-activityType') && document.getElementById('f-activityType').value) || 'group_buy',
            goodsId: goodsId
        };
        if (idEl && idEl.value) {
            body.id = parseInt(idEl.value, 10);
            ConfigAPI.updateSCSkuActivity(body).then(function (res) {
                if (isOk(res)) { alert('更新成功'); closeModal(); loadPanel('sc_sku_activities'); } else { alert(res.info || res.code); }
            });
        } else {
            ConfigAPI.saveSCSkuActivity(body).then(function (res) {
                if (isOk(res)) { alert('保存成功'); closeModal(); loadPanel('sc_sku_activities'); } else { alert(res.info || res.code); }
            });
        }
    }

    function bindPanelEvents() {
        document.querySelectorAll('.nav-item').forEach(function (el) {
            el.addEventListener('click', function () {
                var panelId = this.getAttribute('data-panel');
                document.querySelectorAll('.nav-item').forEach(function (n) { n.classList.remove('active'); });
                document.querySelectorAll('.panel').forEach(function (p) { p.classList.remove('active'); });
                this.classList.add('active');
                var panel = document.getElementById('panel-' + panelId);
                if (panel) panel.classList.add('active');
                loadPanel(panelId);
            });
        });

        document.querySelectorAll('.btn-refresh').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var key = this.getAttribute('data-refresh');
                if (key) loadPanel(key);
            });
        });

        document.querySelectorAll('[data-form]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var formType = this.getAttribute('data-form');
                currentFormType = formType;
                currentEditId = null;
                var title = '新增';
                var body = '';
                if (formType === 'activity_type') { title = '新增活动类型'; body = formActivityType(null); }
                else if (formType === 'category') { title = '新增类目'; body = formCategory(null); }
                else if (formType === 'crowd_tag') { title = '新增人群标签'; body = formCrowdTag(null); }
                else if (formType === 'sku') { title = '新增/编辑商品'; body = formSku(null); }
                else if (formType === 'group_buy_activity') { title = '新增/编辑拼团活动'; body = formGroupBuyActivity(null); }
                else if (formType === 'seckill_activity') { title = '新增/编辑秒杀活动'; body = formSeckillActivity(null); }
                else if (formType === 'group_buy_discount') { title = '新增折扣'; body = formGroupBuyDiscount(null); }
                else if (formType === 'sc_sku_activity') { title = '新增渠道商品活动'; body = formSCSkuActivity(); }
                openModal(title, body, function () {
                    if (formType === 'activity_type') submitActivityType();
                    else if (formType === 'category') submitCategory();
                    else if (formType === 'crowd_tag') submitCrowdTag();
                    else if (formType === 'sku') submitSku();
                    else if (formType === 'group_buy_activity') submitGroupBuyActivity();
                    else if (formType === 'seckill_activity') submitSeckillActivity();
                    else if (formType === 'group_buy_discount') submitGroupBuyDiscount();
                    else if (formType === 'sc_sku_activity') submitSCSkuActivity();
                });
            });
        });

        document.getElementById('modalClose').addEventListener('click', closeModal);
        document.getElementById('modalCancel').addEventListener('click', closeModal);
        document.getElementById('modal').addEventListener('click', function (e) {
            if (e.target === this) closeModal();
        });

        document.getElementById('btnSaveBaseUrl').addEventListener('click', function () {
            var url = document.getElementById('baseUrl').value.trim();
            if (url) { setBaseUrl(url); alert('已保存接口地址'); }
        });

        document.getElementById('btnFilterSc').addEventListener('click', function () {
            loadPanel('sc_sku_activities');
        });

        document.getElementById('btnDccUpdate').addEventListener('click', function () {
            var key = document.getElementById('dcc-key').value.trim();
            var value = document.getElementById('dcc-value').value.trim();
            if (!key) { alert('请输入 key'); return; }
            ConfigAPI.updateDcc(key, value).then(function (res) {
                if (isOk(res)) alert('DCC 更新成功'); else alert(res.info || res.code);
            });
        });

        document.body.addEventListener('click', function (e) {
            var t = e.target;
            if (t.classList.contains('btn-edit-activity-type')) {
                var id = t.getAttribute('data-id');
                ConfigAPI.getActivityType(id).then(function (res) {
                    if (isOk(res) && res.data) {
                        openModal('编辑活动类型', formActivityType(res.data), submitActivityType);
                    }
                });
            }
            if (t.classList.contains('btn-delete-activity-type')) {
                if (!confirm('确定删除该活动类型？')) return;
                ConfigAPI.deleteActivityType(t.getAttribute('data-id')).then(function (res) {
                    if (isOk(res)) { loadPanel('activity_types'); alert('已删除'); } else { alert(res.info || res.code); }
                });
            }
            if (t.classList.contains('btn-edit-category')) {
                var id = t.getAttribute('data-id');
                ConfigAPI.getCategory(id).then(function (res) {
                    if (isOk(res) && res.data) {
                        openModal('编辑类目', formCategory(res.data), submitCategory);
                    }
                });
            }
            if (t.classList.contains('btn-delete-category')) {
                if (!confirm('确定删除该类目？')) return;
                ConfigAPI.deleteCategory(t.getAttribute('data-id')).then(function (res) {
                    if (isOk(res)) { loadPanel('categories'); alert('已删除'); } else { alert(res.info || res.code); }
                });
            }
            if (t.classList.contains('btn-delete-crowd-tag')) {
                if (!confirm('确定删除该人群标签？')) return;
                ConfigAPI.deleteCrowdTag(t.getAttribute('data-tagId')).then(function (res) {
                    if (isOk(res)) { loadPanel('crowd_tags'); alert('已删除'); } else { alert(res.info || res.code); }
                });
            }
            if (t.classList.contains('btn-edit-sc')) {
                var id = t.getAttribute('data-id');
                ConfigAPI.getSCSkuActivity(id).then(function (res) {
                    if (isOk(res) && res.data) {
                        openModal('编辑渠道商品活动', formSCSkuActivity(res.data), submitSCSkuActivity);
                    }
                });
            }
            if (t.classList.contains('btn-edit') && t.getAttribute('data-goodsId')) {
                ConfigAPI.getSku(t.getAttribute('data-goodsId')).then(function (res) {
                    if (isOk(res) && res.data) {
                        currentFormType = 'sku';
                        openModal('编辑商品', formSku(res.data), submitSku);
                    }
                });
            }
            if (t.classList.contains('btn-delete-sku')) {
                if (!confirm('确定删除该商品？')) return;
                ConfigAPI.deleteSku(t.getAttribute('data-goodsId')).then(function (res) {
                    if (isOk(res)) { loadPanel('skus'); alert('已删除'); } else alert(res.info || res.code);
                });
            }
            if (t.classList.contains('btn-edit-activity')) {
                var aid = t.getAttribute('data-activityId');
                ConfigAPI.getGroupBuyActivity(aid).then(function (res) {
                    if (isOk(res) && res.data) {
                        openModal('编辑拼团活动', formGroupBuyActivity(res.data), submitGroupBuyActivity);
                    }
                });
            }
            if (t.classList.contains('btn-status-activity')) {
                var aid = t.getAttribute('data-activityId');
                var status = prompt('状态：0创建 1生效 2过期 3废弃', '1');
                if (status === null) return;
                ConfigAPI.updateGroupBuyActivityStatus(aid, parseInt(status, 10)).then(function (res) {
                    if (isOk(res)) { loadPanel('group_buy_activities'); alert('已更新'); } else alert(res.info || res.code);
                });
            }
            if (t.classList.contains('btn-delete-activity')) {
                if (!confirm('确定删除该拼团活动？')) return;
                ConfigAPI.deleteGroupBuyActivity(t.getAttribute('data-activityId')).then(function (res) {
                    if (isOk(res)) { loadPanel('group_buy_activities'); alert('已删除'); } else alert(res.info || res.code);
                });
            }
            if (t.classList.contains('btn-edit-seckill')) {
                var aid = t.getAttribute('data-activityId');
                ConfigAPI.getSeckillActivity(aid).then(function (res) {
                    if (isOk(res) && res.data) {
                        openModal('编辑秒杀活动', formSeckillActivity(res.data), submitSeckillActivity);
                    }
                });
            }
            if (t.classList.contains('btn-delete-seckill')) {
                if (!confirm('确定删除该秒杀活动？')) return;
                ConfigAPI.deleteSeckillActivity(t.getAttribute('data-activityId')).then(function (res) {
                    if (isOk(res)) { loadPanel('seckill_activities'); alert('已删除'); } else alert(res.info || res.code);
                });
            }
            if (t.classList.contains('btn-edit-discount')) {
                var did = t.getAttribute('data-discountId');
                currentEditId = did;
                ConfigAPI.getGroupBuyDiscount(did).then(function (res) {
                    if (isOk(res) && res.data) {
                        openModal('编辑折扣', formGroupBuyDiscount(res.data), submitGroupBuyDiscount);
                    }
                });
            }
            if (t.classList.contains('btn-delete-discount')) {
                if (!confirm('确定删除该折扣？')) return;
                ConfigAPI.deleteGroupBuyDiscount(t.getAttribute('data-discountId')).then(function (res) {
                    if (isOk(res)) { loadPanel('group_buy_discounts'); alert('已删除'); } else alert(res.info || res.code);
                });
            }
            if (t.classList.contains('btn-delete-sc')) {
                if (!confirm('确定删除该关联？')) return;
                ConfigAPI.deleteSCSkuActivity(t.getAttribute('data-id')).then(function (res) {
                    if (isOk(res)) { loadPanel('sc_sku_activities'); alert('已删除'); } else alert(res.info || res.code);
                });
            }
            if (t.classList.contains('btn-edit-tag-stat')) {
                var tagId = t.getAttribute('data-tagId');
                var statistics = prompt('统计量（覆盖为绝对值）', t.getAttribute('data-statistics') || '0');
                if (statistics === null) return;
                ConfigAPI.updateCrowdTagsStatistics(tagId, parseInt(statistics, 10)).then(function (res) {
                    if (isOk(res)) { loadPanel('crowd_tags'); alert('已更新'); } else alert(res.info || res.code);
                });
            }
        });
    }

    function init() {
        var saved = localStorage.getItem(STORAGE_BASE_URL);
        if (saved && document.getElementById('baseUrl')) document.getElementById('baseUrl').value = saved;
        bindPanelEvents();
        loadPanel('activity_types');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
