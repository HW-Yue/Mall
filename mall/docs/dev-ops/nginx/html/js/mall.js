// 普通商品商城页脚本（接口路径见 js/api-config.js）

(function () {
  const categoryTabsEl = document.getElementById("categoryTabs");
  const goodsListEl = document.getElementById("goodsList");
  const emptyStateEl = document.getElementById("emptyState");
  const pageInfoEl = document.getElementById("pageInfo");
  const goodsSummaryEl = document.getElementById("goodsSummary");
  const prevPageBtn = document.getElementById("prevPage");
  const nextPageBtn = document.getElementById("nextPage");

  const groupBuySectionEl = document.getElementById("groupBuySection");
  const groupBuyListEl = document.getElementById("groupBuyList");
  const seckillSectionEl = document.getElementById("seckillSection");
  const seckillListEl = document.getElementById("seckillList");

  const modalMaskEl = document.getElementById("goodsModalMask");
  const modalCloseEl = document.getElementById("goodsModalClose");
  const modalImageEl = document.getElementById("modalImage");
  const modalTitleEl = document.getElementById("modalTitle");
  const modalDescEl = document.getElementById("modalDesc");
  const modalSpecsEl = document.getElementById("modalSpecs");
  const modalPriceEl = document.getElementById("modalPrice");
  const modalBuyBtnEl = document.getElementById("modalBuyBtn");

  // 秒杀弹窗相关元素
  const seckillModalMaskEl = document.getElementById("seckillModalMask");
  const seckillModalCloseEl = document.getElementById("seckillModalClose");
  const seckillGoodsTitleEl = document.getElementById("seckillGoodsTitle");
  const seckillGoodsSummaryEl = document.getElementById("seckillGoodsSummary");
  const seckillActivityInfoEl = document.getElementById("seckillActivityInfo");
  const seckillPayPriceEl = document.getElementById("seckillPayPrice");
  const seckillBuyBtnEl = document.getElementById("seckillBuyBtn");

  // 拼团弹窗相关元素
  const groupBuyModalMaskEl = document.getElementById("groupBuyModalMask");
  const groupBuyModalCloseEl = document.getElementById("groupBuyModalClose");
  const groupBuyGoodsTitleEl = document.getElementById("groupBuyGoodsTitle");
  const groupBuyGoodsSummaryEl = document.getElementById("groupBuyGoodsSummary");
  const groupBuyTeamStatisticEl = document.getElementById("groupBuyTeamStatistic");
  const groupBuyTeamListEl = document.getElementById("groupBuyTeamList");
  const groupBuyPayPriceEl = document.getElementById("groupBuyPayPrice");
  const groupBuyOpenBtnEl = document.getElementById("groupBuyOpenBtn");

  if (!window.AppConfig || !window.AppUtils || !window.AppApi || !window.AppApiPaths) {
    console.error("请按顺序引入 common.js、api-config.js");
    return;
  }

  const P = window.AppApiPaths;
  const apiMarket = window.AppApi.market.bind(window.AppApi);

  let currentCategoryId = null; // null 表示全部
  let pageNum = 1;
  const pageSize = 10;
  let total = 0;
  let currentList = [];
  let selectedGoods = null;
  let currentGroupBuyItem = null;
  let currentGroupBuyConfig = null;
  let currentSeckillItem = null;

  function getCurrentUserId() {
    return window.AppUtils.getCurrentUserId();
  }

  /** 确认支付页 URL，可带 payAmount 供展示（与 payment.js 读取一致） */
  function buildPaymentPageUrl(orderId, userId, payAmount) {
    var base =
      "payment.html?orderId=" +
      encodeURIComponent(orderId) +
      "&userId=" +
      encodeURIComponent(userId);
    if (payAmount != null && payAmount !== "" && !isNaN(Number(payAmount))) {
      base +=
        "&payAmount=" +
        encodeURIComponent(Number(payAmount).toFixed(2));
    }
    return base;
  }

  function fetchCategories() {
    const url = apiMarket(P.mallIndex.queryCategoryTypeList);
    return fetch(url)
      .then((res) => res.json())
      .then((json) => {
        if (json.code !== "0000") {
          console.error("加载分类失败：", json.info);
          return [];
        }
        return Array.isArray(json.data) ? json.data : [];
      })
      .catch((err) => {
        console.error("加载分类异常：", err);
        return [];
      });
  }

  // 查询活动商品（拼团 / 秒杀）- 分别调用各自服务
  function fetchActivityGoods() {
    const groupBuyUrl = window.AppApi.groupBuy(P.gbm.queryGoodsList);
    const seckillUrl = window.AppApi.seckill(P.seckill.queryGoodsList);

    // 字段映射：将后端字段转换为前端使用的字段
    function mapGoodsItem(item) {
      return {
        id: item.goodsId,
        goodsName: item.goodsName,
        imageUrl: item.goodsImageUrl,
        source: item.source,
        channel: item.channel,
        originalPrice: item.originalPrice,
        payPrice: item.payPrice,
        activityId: item.activityId,
      };
    }

    const groupBuyPromise = fetch(groupBuyUrl)
      .then((res) => res.json())
      .then((json) => {
        if (json.code !== "0000") {
          console.error("加载拼团商品失败：", json.info);
          return [];
        }
        const data = json.data || {};
        const list = Array.isArray(data.groupBuyGoodsList) ? data.groupBuyGoodsList : [];
        return list.map(mapGoodsItem);
      })
      .catch((err) => {
        console.error("加载拼团商品异常：", err);
        return [];
      });

    const seckillPromise = fetch(seckillUrl)
      .then((res) => res.json())
      .then((json) => {
        if (json.code !== "0000") {
          console.error("加载秒杀商品失败：", json.info);
          return [];
        }
        const data = json.data || {};
        const list = Array.isArray(data.seckillGoodsList) ? data.seckillGoodsList : [];
        return list.map(mapGoodsItem);
      })
      .catch((err) => {
        console.error("加载秒杀商品异常：", err);
        return [];
      });

    return Promise.all([groupBuyPromise, seckillPromise]).then(([groupBuyList, seckillList]) => {
      return { groupBuyList, seckillList };
    });
  }

  function renderActivityList(sectionEl, listEl, list, type) {
    if (!sectionEl || !listEl) return;

    listEl.innerHTML = "";

    if (!list || !list.length) {
      sectionEl.style.display = "none";
      return;
    }

    sectionEl.style.display = "block";

    list.forEach((item) => {
      const card = document.createElement("div");
      card.className = "mall-activity-card";

      const imgBox = document.createElement("div");
      imgBox.className = "mall-activity-card-image";
      const img = document.createElement("img");
      img.src = item.imageUrl || "./images/sku-13811216-04.png";
      img.alt = item.goodsName || "";
      imgBox.appendChild(img);

      const body = document.createElement("div");
      body.className = "mall-activity-card-body";

      const titleRow = document.createElement("div");
      const title = document.createElement("div");
      title.className = "mall-activity-card-title";
      title.textContent = item.goodsName || "";

      const tag = document.createElement("span");
      tag.className =
        "mall-activity-tag " + (type === "group_buy" ? "group" : "seckill");
      tag.textContent = type === "group_buy" ? "拼团中" : "限时秒杀";

      titleRow.style.display = "flex";
      titleRow.style.justifyContent = "space-between";
      titleRow.style.alignItems = "center";
      titleRow.style.gap = "6px";
      titleRow.appendChild(title);
      titleRow.appendChild(tag);

      const metaRow = document.createElement("div");
      metaRow.className = "mall-activity-card-meta";
      const sourceSpan = document.createElement("span");
      sourceSpan.textContent = item.source ? "来源 " + item.source : "标准商品";
      const channelSpan = document.createElement("span");
      channelSpan.textContent = item.channel ? "渠道 " + item.channel : "";
      metaRow.appendChild(sourceSpan);
      metaRow.appendChild(channelSpan);

      const priceEl = document.createElement("div");
      priceEl.className = "mall-activity-card-price";
      if (item.originalPrice != null) {
        priceEl.textContent =
          "￥" + Number(item.originalPrice || 0).toFixed(2);
      } else {
        priceEl.textContent = "活动价";
      }

      body.appendChild(titleRow);
      body.appendChild(metaRow);
      body.appendChild(priceEl);

      card.appendChild(imgBox);
      card.appendChild(body);

      if (type === "group_buy") {
        card.style.cursor = "pointer";
        card.addEventListener("click", function () {
          openGroupBuyModal(item);
        });
      } else if (type === "seckill") {
        card.style.cursor = "pointer";
        card.addEventListener("click", function () {
          openSeckillModal(item);
        });
      }

      listEl.appendChild(card);
    });
  }

  function renderCategories(categories) {
    categoryTabsEl.innerHTML = "";

    const allTab = document.createElement("button");
    allTab.className = "mall-category-tab active";
    allTab.dataset.id = "";
    allTab.textContent = "全部";
    categoryTabsEl.appendChild(allTab);

    categories.forEach((c) => {
      const btn = document.createElement("button");
      btn.className = "mall-category-tab";
      btn.dataset.id = c.id;
      btn.textContent = c.name || ("类型" + c.id);
      categoryTabsEl.appendChild(btn);
    });

    categoryTabsEl.addEventListener("click", function (e) {
      const target = e.target.closest(".mall-category-tab");
      if (!target) return;
      const id = target.dataset.id;
      currentCategoryId = id === "" ? null : id;
      pageNum = 1;

      categoryTabsEl.querySelectorAll(".mall-category-tab").forEach((el) => {
        el.classList.toggle("active", el === target);
      });

      loadGoods();
    });
  }

  function fetchGoods() {
    const url = apiMarket(P.mallIndex.queryGoodsPage);
    const body = {
      categoryId: currentCategoryId ? Number(currentCategoryId) : null,
      pageNum: pageNum,
      pageSize: pageSize,
    };

    return fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((json) => {
        if (json.code !== "0000") {
          console.error("加载商品失败：", json.info);
          return { total: 0, list: [] };
        }
        return json.data || { total: 0, list: [] };
      })
      .catch((err) => {
        console.error("加载商品异常：", err);
        return { total: 0, list: [] };
      });
  }

  function renderGoods(data) {
    total = data.total || 0;
    const list = Array.isArray(data.list) ? data.list : [];
    currentList = list;

    goodsListEl.innerHTML = "";

    if (!list.length) {
      goodsListEl.style.display = "none";
      emptyStateEl.style.display = "block";
    } else {
      goodsListEl.style.display = "grid";
      emptyStateEl.style.display = "none";

      list.forEach((g) => {
        const card = document.createElement("div");
        card.className = "mall-card";

        const imgBox = document.createElement("div");
        imgBox.className = "mall-card-image";

        const img = document.createElement("img");
        img.src = g.imageUrl || "./images/sku-13811216-01.png";
        img.alt = g.name || "";
        imgBox.appendChild(img);

        const body = document.createElement("div");
        body.className = "mall-card-body";

        const title = document.createElement("div");
        title.className = "mall-card-title";
        title.textContent = g.name || "";

        const meta = document.createElement("div");
        meta.className = "mall-card-meta";
        const sourceSpan = document.createElement("span");
        sourceSpan.textContent = g.source ? "来源 " + g.source : "标准商品";
        const channelSpan = document.createElement("span");
        channelSpan.textContent = g.channel ? "渠道 " + g.channel : "";
        meta.appendChild(sourceSpan);
        meta.appendChild(channelSpan);

        const priceRow = document.createElement("div");
        priceRow.className = "mall-card-price";
        const priceMain = document.createElement("span");
        priceMain.className = "mall-card-price-main";
        priceMain.textContent = "￥" + Number(g.price || 0).toFixed(2);
        const priceSub = document.createElement("span");
        priceSub.className = "mall-card-price-sub";
        priceSub.textContent = "含税 / 立即到账";
        priceRow.appendChild(priceMain);
        priceRow.appendChild(priceSub);

        const footer = document.createElement("div");
        footer.className = "mall-card-footer";
        const detailSpan = document.createElement("span");
        detailSpan.style.fontSize = "11px";
        detailSpan.style.color = "#3b82f6";
        detailSpan.textContent = "查看详情";
        const buyBtn = document.createElement("button");
        buyBtn.className = "mall-card-buy-btn";
        buyBtn.textContent = "立即购买";
        footer.appendChild(detailSpan);
        footer.appendChild(buyBtn);

        body.appendChild(title);
        body.appendChild(meta);
        body.appendChild(priceRow);
        body.appendChild(footer);

        card.appendChild(imgBox);
        card.appendChild(body);

        card.addEventListener("click", () => openGoodsModal(g));
        buyBtn.addEventListener("click", (e) => {
          e.stopPropagation();
          openGoodsModal(g, true);
        });

        goodsListEl.appendChild(card);
      });
    }

    const pageCount = total > 0 ? Math.ceil(total / pageSize) : 1;
    const safePageNum = Math.min(pageNum, pageCount);
    if (safePageNum !== pageNum) {
      pageNum = safePageNum;
    }

    pageInfoEl.textContent = pageNum + " / " + pageCount;
    prevPageBtn.disabled = pageNum <= 1;
    nextPageBtn.disabled = pageNum >= pageCount;

    if (goodsSummaryEl) {
      goodsSummaryEl.textContent = total
        ? "共 " + total + " 件商品"
        : "暂无商品";
    }
  }

  function loadGoods() {
    fetchGoods().then(renderGoods);
  }

  function fetchGoodsDetail(goodsId) {
    if (!goodsId) return Promise.resolve(null);
    const url = apiMarket(P.mallIndex.querySkuDetail);
    const body = {
      goodsId: goodsId,
    };
    return fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((json) => {
        if (json.code !== "0000" || !json.data) {
          console.error("加载商品详情失败：", json.info);
          return null;
        }
        return json.data;
      })
      .catch((err) => {
        console.error("加载商品详情异常：", err);
        return null;
      });
  }

  function openGoodsModal(goods, autoBuy) {
    selectedGoods = goods;

    function renderDetail(detail) {
      const data = detail || goods || {};
      modalImageEl.src = data.imageUrl || "./images/sku-13811216-01.png";
      modalTitleEl.textContent = data.name || "";
      modalDescEl.textContent =
        (data.goodsDetail ||
          data.description ||
          data.desc ||
          "高可用 · 高性价比 · 支持企业级接入") + "";

      modalPriceEl.textContent =
        "￥" + Number(data.price || 0).toFixed(2);

      modalSpecsEl.innerHTML = "";
      const specs = {
        来源: data.source || "s01",
        渠道: data.channel || "c01",
        ID: data.id || "",
      };
      Object.keys(specs).forEach((k) => {
        const wrap = document.createElement("div");
        const kEl = document.createElement("div");
        kEl.className = "mall-modal-spec-key";
        kEl.textContent = k;
        const vEl = document.createElement("div");
        vEl.className = "mall-modal-spec-value";
        vEl.textContent = specs[k];
        wrap.appendChild(kEl);
        wrap.appendChild(vEl);
        modalSpecsEl.appendChild(wrap);
      });
    }

    // 先用列表数据渲染一版，保证体验
    renderDetail(null);

    // 再根据商品ID补充详情信息
    fetchGoodsDetail(goods.id).then((detail) => {
      if (!detail) return;
      selectedGoods = Object.assign({}, goods, detail);
      renderDetail(detail);
    });

    modalMaskEl.classList.add("show");
    modalMaskEl.style.display = "flex";

    if (autoBuy) {
      createPayOrder();
    }
  }

  function closeGoodsModal() {
    modalMaskEl.classList.remove("show");
    modalMaskEl.style.display = "none";
    selectedGoods = null;
  }

  // 查询拼团营销配置：按用户 + 商品
  function fetchGroupBuyMarketConfig(activityItem) {
    const userId = getCurrentUserId();
    if (!userId) return Promise.resolve(null);

    const url = window.AppApi.groupBuy(P.gbm.queryGroupBuyMarketConfig);
    const body = {
      userId: userId,
      source: activityItem.source || "s01",
      channel: activityItem.channel || "c01",
      goodsId: activityItem.goodsId || activityItem.id,
    };

    return fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((json) => {
        if (json.code !== "0000") {
          console.error("加载拼团营销配置失败：", json.info);
          alert(json.info || "拼团配置加载失败，请稍后重试");
          return null;
        }
        return json.data || null;
      })
      .catch((err) => {
        console.error("加载拼团营销配置异常：", err);
        alert("拼团配置加载异常，请稍后重试");
        return null;
      });
  }

  function closeGroupBuyModal() {
    if (!groupBuyModalMaskEl) return;
    groupBuyModalMaskEl.classList.remove("show");
    groupBuyModalMaskEl.style.display = "none";
    currentGroupBuyItem = null;
    currentGroupBuyConfig = null;
  }

  // 打开秒杀弹窗
  function openSeckillModal(item) {
    if (!seckillModalMaskEl) return;
    currentSeckillItem = item;

    if (seckillGoodsTitleEl) seckillGoodsTitleEl.textContent = item.goodsName || "";
    if (seckillGoodsSummaryEl) seckillGoodsSummaryEl.textContent = item.goodsName || "";
    if (seckillActivityInfoEl) seckillActivityInfoEl.textContent = "活动ID：" + (item.activityId || "-");
    if (seckillPayPriceEl) {
      const price = item.payPrice != null ? item.payPrice : item.originalPrice;
      seckillPayPriceEl.textContent = "￥" + Number(price || 0).toFixed(2);
    }

    seckillModalMaskEl.style.display = "flex";
    seckillModalMaskEl.classList.add("show");
  }

  function closeSeckillModal() {
    if (!seckillModalMaskEl) return;
    seckillModalMaskEl.classList.remove("show");
    seckillModalMaskEl.style.display = "none";
    currentSeckillItem = null;
  }

  function createSeckillPayOrder() {
    const userId = getCurrentUserId();
    if (!userId || !currentSeckillItem) return;

    const url = window.AppApi.seckill(P.seckill.createPayOrder);
    const body = {
      userId: userId,
      productId: String(currentSeckillItem.id),
      activityId: currentSeckillItem.activityId,
      source: currentSeckillItem.source || "s01",
      channel: currentSeckillItem.channel || "c01",
      goodsName: currentSeckillItem.goodsName || "",
      goodsImageUrl: currentSeckillItem.imageUrl || "",
    };

    fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((json) => {
        if (json.code === "0000" && json.data) {
          const seckillToken = json.data.seckillToken;
          if (seckillToken) {
            window.localStorage.setItem("lastSeckillToken", seckillToken);
            var est =
              currentSeckillItem.payPrice != null
                ? currentSeckillItem.payPrice
                : currentSeckillItem.originalPrice;
            var waitUrl =
              "seckill-wait.html?seckillToken=" +
              encodeURIComponent(seckillToken) +
              "&userId=" +
              encodeURIComponent(userId);
            if (est != null && !isNaN(Number(est))) {
              waitUrl +=
                "&payAmount=" + encodeURIComponent(Number(est).toFixed(2));
            }
            window.location.href = waitUrl;
            return;
          }
          alert("秒杀请求已受理，但未返回 token");
        } else {
          console.error("秒杀下单失败：", json.info);
          alert("秒杀下单失败：" + (json.info || "请稍后重试"));
        }
      })
      .catch((err) => {
        console.error("秒杀下单异常：", err);
        alert("网络异常，请稍后重试");
      });
  }

  // 打开拼团详情弹窗
  function openGroupBuyModal(activityItem) {
    if (!groupBuyModalMaskEl) return;

    currentGroupBuyItem = activityItem;
    currentGroupBuyConfig = null;

    if (groupBuyGoodsTitleEl) {
      groupBuyGoodsTitleEl.textContent = activityItem.goodsName || "";
    }
    if (groupBuyGoodsSummaryEl) {
      groupBuyGoodsSummaryEl.textContent = "正在为你加载拼团信息...";
    }
    if (groupBuyTeamStatisticEl) {
      groupBuyTeamStatisticEl.textContent = "";
    }
    if (groupBuyTeamListEl) {
      groupBuyTeamListEl.innerHTML = "";
    }
    if (groupBuyPayPriceEl) {
      groupBuyPayPriceEl.textContent = "";
    }

    groupBuyModalMaskEl.style.display = "flex";
    groupBuyModalMaskEl.classList.add("show");

    fetchGroupBuyMarketConfig(activityItem).then((config) => {
      if (!config) {
        if (groupBuyGoodsSummaryEl) {
          groupBuyGoodsSummaryEl.textContent = "暂时无法获取拼团配置，请稍后重试。";
        }
        return;
      }
      currentGroupBuyConfig = config;

      const goods = config.goods || {};
      const teamStatistic = config.teamStatistic || {};
      const teamList = Array.isArray(config.teamList) ? config.teamList : [];

      if (groupBuyGoodsTitleEl) {
        groupBuyGoodsTitleEl.textContent =
          goods.goodsName || activityItem.goodsName || "";
      }

      if (groupBuyGoodsSummaryEl) {
        const original = goods.originalPrice != null ? Number(goods.originalPrice) : null;
        const deduction = goods.deductionPrice != null ? Number(goods.deductionPrice) : null;
        const pay = goods.payPrice != null ? Number(goods.payPrice) : null;
        let summary = "";
        if (original != null) {
          summary += "原价 ￥" + original.toFixed(2);
        }
        if (deduction != null) {
          summary += (summary ? "，" : "") + "立减 ￥" + deduction.toFixed(2);
        }
        if (pay != null) {
          summary += (summary ? "，" : "") + "拼团价 ￥" + pay.toFixed(2);
        }
        groupBuyGoodsSummaryEl.textContent =
          summary || "拼团优惠信息加载完成";
      }

      if (groupBuyTeamStatisticEl) {
        const allTeamCount = teamStatistic.allTeamCount || 0;
        const allTeamCompleteCount = teamStatistic.allTeamCompleteCount || 0;
        const allTeamUserCount = teamStatistic.allTeamUserCount || 0;
        groupBuyTeamStatisticEl.textContent =
          "累计开团 " +
          allTeamCount +
          "，已成团 " +
          allTeamCompleteCount +
          "，参团人数 " +
          allTeamUserCount;
      }

      if (groupBuyPayPriceEl) {
        const pay = goods.payPrice != null ? Number(goods.payPrice) : null;
        const basePrice =
          pay != null
            ? pay
            : goods.originalPrice != null
            ? Number(goods.originalPrice)
            : 0;
        groupBuyPayPriceEl.textContent = "￥" + basePrice.toFixed(2);
      }

      if (groupBuyTeamListEl) {
        groupBuyTeamListEl.innerHTML = "";

        if (!teamList.length) {
          const empty = document.createElement("div");
          empty.style.fontSize = "12px";
          empty.style.color = "#6b7280";
          empty.textContent = "当前暂无进行中的团，快来自己开团吧～";
          groupBuyTeamListEl.appendChild(empty);
        } else {
          teamList.forEach((team) => {
            const row = document.createElement("div");
            row.style.display = "flex";
            row.style.alignItems = "center";
            row.style.justifyContent = "space-between";
            row.style.marginBottom = "6px";

            const info = document.createElement("div");
            info.style.fontSize = "12px";
            info.style.color = "#4b5563";
            const target = team.targetCount || 0;
            const complete = team.completeCount || 0;
            info.textContent =
              "团号 " +
              (team.teamId || "") +
              "，进度 " +
              complete +
              "/" +
              target;

            const btn = document.createElement("button");
            btn.className = "mall-card-buy-btn";
            btn.style.fontSize = "11px";
            btn.textContent = "去参团";
            btn.addEventListener("click", function () {
              createGroupBuyPayOrder("join", team);
            });

            row.appendChild(info);
            row.appendChild(btn);
            groupBuyTeamListEl.appendChild(row);
          });
        }
      }
    });
  }

  function createPayOrder() {
    const userId = getCurrentUserId();
    if (!userId) return;
    if (!selectedGoods) return;

    const marketType = (selectedGoods.marketType && String(selectedGoods.marketType)) || "normal";

    let url, body;
    if (marketType === "seckill") {
      url = window.AppApi.seckill(P.seckill.createPayOrder);
      body = {
        userId: userId,
        productId: String(selectedGoods.id),
        activityId: selectedGoods.activityId,
        source: selectedGoods.source || "s01",
        channel: selectedGoods.channel || "c01",
        goodsName: selectedGoods.goodsName || "",
        goodsImageUrl: selectedGoods.imageUrl || "",
      };
    } else {
      url = window.AppApi.market(P.mallTrade.createNormalOrder);
      body = {
        userId: userId,
        productId: String(selectedGoods.id),
        originalPrice: selectedGoods.price || 0,
        deductionPrice: 0,
        payPrice: selectedGoods.price || 0,
        source: selectedGoods.source || "s01",
        channel: selectedGoods.channel || "c01",
        goodsName: selectedGoods.goodsName || "",
        goodsImageUrl: selectedGoods.imageUrl || null,
      };
    }

    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((json) => {
        if (json.code === "0000" && json.data) {
          if (marketType === "seckill") {
            const seckillToken = json.data.seckillToken;
            if (seckillToken) {
              window.localStorage.setItem("lastSeckillToken", seckillToken);
              var est2 =
                selectedGoods.payPrice != null
                  ? selectedGoods.payPrice
                  : selectedGoods.originalPrice;
              var waitUrl2 =
                "seckill-wait.html?seckillToken=" +
                encodeURIComponent(seckillToken) +
                "&userId=" +
                encodeURIComponent(userId);
              if (est2 != null && !isNaN(Number(est2))) {
                waitUrl2 +=
                  "&payAmount=" +
                  encodeURIComponent(Number(est2).toFixed(2));
              }
              window.location.href = waitUrl2;
              return;
            }
            alert("秒杀请求已受理，但未返回 token");
          } else {
            const orderId = json.data.orderId;
            window.location.href = buildPaymentPageUrl(
              orderId,
              userId,
              selectedGoods.price != null
                ? selectedGoods.price
                : body.payPrice
            );
          }
        } else {
          console.error("创建支付订单失败：", json.info);
          alert("下单失败：" + (json.info || "请稍后重试"));
        }
      })
      .catch((err) => {
        console.error("创建支付订单异常：", err);
        alert("网络异常，请稍后重试");
      });
  }

  // 拼团下单；action: "open" 自己开团；"join" 参团（带 teamId）
  function createGroupBuyPayOrder(action, team) {
    const userId = getCurrentUserId();
    if (!userId) return;
    if (!currentGroupBuyItem || !currentGroupBuyConfig) {
      alert("拼团信息未就绪，请稍后重试");
      return;
    }

    const goods = currentGroupBuyConfig.goods || {};
    const activityId = currentGroupBuyConfig.activityId;
    const productId =
      goods.goodsId ||
      goods.id ||
      currentGroupBuyItem.goodsId ||
      currentGroupBuyItem.id;

    if (!activityId || !productId) {
      alert("拼团配置不完整，无法下单");
      return;
    }

    const url = window.AppApi.groupBuy(P.gbm.createPayOrder);
    const body = {
      userId: userId,
      productId: String(productId),
      activityId: activityId,
      source: currentGroupBuyItem.source || "s01",
      channel: currentGroupBuyItem.channel || "c01",
    };

    // 参团时带 teamId；自己开团不带 teamId
    if (action === "join" && team && team.teamId) {
      body.teamId = team.teamId;
    }

    fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((json) => {
        if (json.code === "0000" && json.data) {
          const orderId = json.data.orderId;
          const g = (currentGroupBuyConfig && currentGroupBuyConfig.goods) || {};
          const payAmt =
            g.payPrice != null ? g.payPrice : g.originalPrice != null ? g.originalPrice : null;
          window.location.href = buildPaymentPageUrl(orderId, userId, payAmt);
        } else {
          console.error("拼团创建支付订单失败：", json.info);
          alert("拼团下单失败：" + (json.info || "请稍后重试"));
        }
      })
      .catch((err) => {
        console.error("拼团创建支付订单异常：", err);
        alert("网络异常，请稍后重试");
      });
  }

  document.addEventListener("DOMContentLoaded", function () {
    const userId = getCurrentUserId();
    if (!userId) {
      return;
    }

    fetchCategories().then((categories) => {
      renderCategories(categories);
      loadGoods();
    });

    // 加载活动商品：超值拼团 + 整点秒杀
    fetchActivityGoods().then((data) => {
      renderActivityList(
        groupBuySectionEl,
        groupBuyListEl,
        data.groupBuyList,
        "group_buy"
      );
      renderActivityList(
        seckillSectionEl,
        seckillListEl,
        data.seckillList,
        "seckill"
      );
    });

    prevPageBtn.addEventListener("click", function () {
      if (pageNum <= 1) return;
      pageNum--;
      loadGoods();
    });
    nextPageBtn.addEventListener("click", function () {
      const pageCount = total > 0 ? Math.ceil(total / pageSize) : 1;
      if (pageNum >= pageCount) return;
      pageNum++;
      loadGoods();
    });

    modalCloseEl.addEventListener("click", closeGoodsModal);
    modalMaskEl.addEventListener("click", function (e) {
      if (e.target === modalMaskEl) {
        closeGoodsModal();
      }
    });
    modalBuyBtnEl.addEventListener("click", function () {
      createPayOrder();
    });

    // 拼团弹窗事件
    if (groupBuyModalCloseEl && groupBuyModalMaskEl) {
      groupBuyModalCloseEl.addEventListener("click", closeGroupBuyModal);
      groupBuyModalMaskEl.addEventListener("click", function (e) {
        if (e.target === groupBuyModalMaskEl) {
          closeGroupBuyModal();
        }
      });
    }
    if (groupBuyOpenBtnEl) {
      groupBuyOpenBtnEl.addEventListener("click", function () {
        createGroupBuyPayOrder("open");
      });
    }

    // 秒杀弹窗事件
    if (seckillModalCloseEl && seckillModalMaskEl) {
      seckillModalCloseEl.addEventListener("click", closeSeckillModal);
      seckillModalMaskEl.addEventListener("click", function (e) {
        if (e.target === seckillModalMaskEl) {
          closeSeckillModal();
        }
      });
    }
    if (seckillBuyBtnEl) {
      seckillBuyBtnEl.addEventListener("click", createSeckillPayOrder);
    }
  });
})();

