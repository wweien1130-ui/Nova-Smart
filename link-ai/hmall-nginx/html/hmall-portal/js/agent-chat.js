/**
 * LinkAI 智能购物助手 - 浮动聊天机器人
 * 可嵌入到灵可商城任何页面右下角
 * 自包含，无外部依赖，不会与 Vue 等框架冲突
 */
(function () {
    'use strict';

    console.log('[LinkAI] 脚本开始加载...');

    // ===== 配置 =====
    const CONFIG = {
        API_BASE: '/api',  // 通过 nginx 代理到后端
        CHAT_ENDPOINT: '/chat',
        STOP_ENDPOINT: '/chat/stop',
        SESSION_ENDPOINT: '/session',
        WELCOME_MESSAGE: '你好！我是 LinkAI 智能购物助手，可以帮你推荐商品、搜索商品、添加购物车等，有什么可以帮助你的吗？',
        // 拖拽相关配置
        DRAG: {
            // 默认位置（右下角）
            defaultRight: 30,
            defaultBottom: 30,
            // 面板相对于按钮的偏移
            panelOffsetX: 0,
            panelOffsetY: -340, // 面板在按钮上方
        }
    };

    console.log('[LinkAI] 配置:', CONFIG);

    // ===== 样式注入 =====
    const styles = `
        /* 浮动按钮 - 可拖拽 */
        .linkai-float-btn {
            position: fixed;
            width: 60px;
            height: 60px;
            border-radius: 50%;
            background: linear-gradient(135deg, #4f46e5, #7c3aed);
            color: #fff;
            border: none;
            cursor: grab;
            box-shadow: 0 4px 20px rgba(79, 70, 229, 0.4);
            z-index: 99999;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;
            transition: box-shadow 0.3s ease;
            animation: linkai-float-in 0.5s ease;
            user-select: none;
            -webkit-user-select: none;
            touch-action: none;
        }
        .linkai-float-btn:hover {
            box-shadow: 0 6px 25px rgba(79, 70, 229, 0.5);
        }
        .linkai-float-btn.linkai-dragging {
            cursor: grabbing;
            box-shadow: 0 8px 30px rgba(79, 70, 229, 0.6);
            transition: none;
        }
        .linkai-float-btn .linkai-badge {
            position: absolute;
            top: -4px;
            right: -4px;
            width: 20px;
            height: 20px;
            background: #ef4444;
            border-radius: 50%;
            font-size: 11px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
            font-weight: 600;
            display: none;
        }
        .linkai-float-btn.linkai-hidden {
            display: none;
        }

        @keyframes linkai-float-in {
            from { opacity: 0; transform: scale(0.5) translateY(20px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }

        /* 聊天面板 - 跟随按钮位置 */
        .linkai-panel {
            position: fixed;
            width: 380px;
            height: 600px;
            max-height: calc(100vh - 140px);
            background: #fff;
            border-radius: 16px;
            box-shadow: 0 8px 40px rgba(0,0,0,0.15);
            z-index: 99998;
            display: none;
            flex-direction: column;
            overflow: hidden;
            animation: linkai-panel-in 0.3s ease;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        }
        .linkai-panel.linkai-show {
            display: flex;
        }

        @keyframes linkai-panel-in {
            from { opacity: 0; transform: translateY(20px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }

        /* 面板头部 */
        .linkai-header {
            padding: 16px 20px;
            background: linear-gradient(135deg, #4f46e5, #7c3aed);
            color: #fff;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-shrink: 0;
        }
        .linkai-header-left {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .linkai-header-left .linkai-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: rgba(255,255,255,0.2);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
        }
        .linkai-header-info h3 {
            margin: 0;
            font-size: 15px;
            font-weight: 600;
        }
        .linkai-header-info p {
            margin: 2px 0 0;
            font-size: 11px;
            opacity: 0.8;
        }
        .linkai-header-close {
            background: none;
            border: none;
            color: #fff;
            font-size: 22px;
            cursor: pointer;
            opacity: 0.7;
            padding: 4px;
            line-height: 1;
            transition: opacity 0.2s;
        }
        .linkai-header-close:hover {
            opacity: 1;
        }

        /* 消息区域 */
        .linkai-messages {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            display: flex;
            flex-direction: column;
            gap: 12px;
            background: #f8f9fc;
        }

        .linkai-msg {
            display: flex;
            gap: 8px;
            max-width: 90%;
            animation: linkai-msg-in 0.3s ease;
        }
        @keyframes linkai-msg-in {
            from { opacity: 0; transform: translateY(8px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .linkai-msg.linkai-user {
            align-self: flex-end;
            flex-direction: row-reverse;
        }
        .linkai-msg.linkai-assistant {
            align-self: flex-start;
        }
        .linkai-msg .linkai-msg-avatar {
            width: 30px;
            height: 30px;
            border-radius: 50%;
            flex-shrink: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
        }
        .linkai-msg.linkai-user .linkai-msg-avatar {
            background: #4f46e5;
            color: #fff;
        }
        .linkai-msg.linkai-assistant .linkai-msg-avatar {
            background: #e0e0e0;
            color: #666;
            border: 1px solid #d0d0d0;
        }
        .linkai-msg .linkai-msg-bubble {
            padding: 10px 14px;
            border-radius: 14px;
            font-size: 13px;
            line-height: 1.5;
            word-break: break-word;
        }
        .linkai-msg.linkai-user .linkai-msg-bubble {
            background: #4f46e5;
            color: #fff;
            border-bottom-right-radius: 4px;
        }
        .linkai-msg.linkai-assistant .linkai-msg-bubble {
            background: #fff;
            color: #333;
            border: 1px solid #eee;
            border-bottom-left-radius: 4px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.04);
        }

        .linkai-msg-bubble p { margin-bottom: 6px; }
        .linkai-msg-bubble p:last-child { margin-bottom: 0; }
        .linkai-msg-bubble ul, .linkai-msg-bubble ol { padding-left: 18px; margin-bottom: 6px; }
        .linkai-msg-bubble li { margin-bottom: 3px; }
        .linkai-msg-bubble code {
            background: #f3f4f6;
            padding: 1px 5px;
            border-radius: 3px;
            font-size: 12px;
            font-family: 'Consolas', 'Monaco', monospace;
        }
        .linkai-msg-bubble pre {
            background: #f3f4f6;
            padding: 10px;
            border-radius: 6px;
            overflow-x: auto;
            margin-bottom: 6px;
            font-size: 12px;
        }
        .linkai-msg-bubble pre code { background: none; padding: 0; }
        .linkai-msg-bubble h1, .linkai-msg-bubble h2, .linkai-msg-bubble h3 {
            margin-bottom: 6px;
            color: #1a1a2e;
            font-size: 14px;
        }
        .linkai-msg-bubble img {
            max-width: 100%;
            max-height: 200px;
            border-radius: 6px;
            margin: 6px 0;
        }
        .linkai-msg-bubble strong { color: #1a1a2e; }

        /* 商品卡片 */
        .linkai-product-cards {
            margin-top: 8px;
            max-width: 100%;
            overflow: hidden;
        }
        .linkai-product-card {
            border: 1px solid #eee;
            border-radius: 10px;
            padding: 12px;
            background: #f9fafb;
            margin-bottom: 8px;
        }
        .linkai-product-card:last-child { margin-bottom: 0; }
        .linkai-product-card-inner {
            display: flex;
            flex-direction: column;
            gap: 8px;
            max-width: 100%;
        }
        .linkai-product-card img {
            width: 100% !important;
            height: 140px !important;
            object-fit: contain !important;
            border-radius: 6px;
            background: #fff;
            border: 1px solid #eee;
            display: block;
        }
        .linkai-product-img-wrapper {
            width: 100%;
            height: 140px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #fff;
            border: 1px solid #eee;
            border-radius: 6px;
            overflow: hidden;
        }
        .linkai-product-img-wrapper img {
            width: 100% !important;
            height: 100% !important;
            object-fit: contain !important;
            display: block;
        }
        .linkai-product-info {
            width: 100%;
            overflow: hidden;
            word-break: break-word;
        }
        .linkai-product-name {
            font-weight: 600;
            font-size: 14px;
            color: #1a1a2e;
            margin-bottom: 4px;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .linkai-product-price {
            font-size: 16px;
            color: #ef4444;
            font-weight: 600;
            margin-bottom: 4px;
        }
        .linkai-product-meta {
            font-size: 11px;
            color: #6b7280;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        /* 打字指示器 */
        .linkai-typing {
            display: flex;
            gap: 4px;
            padding: 8px 0;
        }
        .linkai-typing span {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #bbb;
            animation: linkai-typing 1.4s infinite ease-in-out;
        }
        .linkai-typing span:nth-child(2) { animation-delay: 0.2s; }
        .linkai-typing span:nth-child(3) { animation-delay: 0.4s; }
        @keyframes linkai-typing {
            0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
            30% { transform: translateY(-5px); opacity: 1; }
        }

        .linkai-streaming-cursor::after {
            content: '▊';
            animation: linkai-blink 1s step-end infinite;
            color: #4f46e5;
        }
        @keyframes linkai-blink {
            50% { opacity: 0; }
        }

        /* 输入区域 */
        .linkai-input-area {
            padding: 12px 16px;
            border-top: 1px solid #e8e8e8;
            background: #fff;
            flex-shrink: 0;
        }
        .linkai-input-wrapper {
            display: flex;
            gap: 8px;
            align-items: flex-end;
            background: #f7f8fa;
            border: 1px solid #e0e0e0;
            border-radius: 10px;
            padding: 6px 10px;
            transition: border-color 0.2s;
        }
        .linkai-input-wrapper:focus-within {
            border-color: #4f46e5;
            background: #fff;
        }
        .linkai-input-wrapper textarea {
            flex: 1;
            border: none;
            background: transparent;
            resize: none;
            font-size: 13px;
            line-height: 1.4;
            outline: none;
            font-family: inherit;
            max-height: 80px;
            min-height: 22px;
            padding: 2px 0;
            color: #333;
        }
        .linkai-btn-send {
            background: #4f46e5;
            color: #fff;
            border: none;
            width: 32px;
            height: 32px;
            border-radius: 8px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background 0.2s;
            flex-shrink: 0;
            font-size: 14px;
        }
        .linkai-btn-send:hover { background: #4338ca; }
        .linkai-btn-send:disabled { background: #c7d2fe; cursor: not-allowed; }

        .linkai-btn-stop {
            background: #ef4444;
            color: #fff;
            border: none;
            width: 32px;
            height: 32px;
            border-radius: 8px;
            cursor: pointer;
            display: none;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            font-size: 14px;
        }
        .linkai-btn-stop:hover { background: #dc2626; }

        /* 欢迎信息中的示例问题 */
        .linkai-examples {
            display: flex;
            flex-direction: column;
            gap: 6px;
            margin-top: 6px;
            width: 100%;
        }
        .linkai-example-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 14px;
            background: #fff;
            border: 1px solid #eee;
            border-radius: 10px;
            cursor: pointer;
            transition: all 0.2s ease;
            font-size: 13px;
            color: #555;
        }
        .linkai-example-item:hover {
            border-color: #4f46e5;
            background: #f8f7ff;
            color: #4f46e5;
        }
        .linkai-example-icon { font-size: 18px; flex-shrink: 0; }

        /* 滚动条 */
        .linkai-messages::-webkit-scrollbar { width: 4px; }
        .linkai-messages::-webkit-scrollbar-track { background: transparent; }
        .linkai-messages::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 2px; }

        /* 响应式 */
        @media (max-width: 480px) {
            .linkai-panel {
                right: 10px;
                bottom: 80px;
                width: calc(100vw - 20px);
                height: calc(100vh - 100px);
                max-height: calc(100vh - 100px);
                border-radius: 12px;
            }
            .linkai-float-btn {
                bottom: 20px;
                right: 20px;
                width: 52px;
                height: 52px;
                font-size: 24px;
            }
        }
    `;

    // ===== 注入样式 =====
    function injectStyles() {
        const styleEl = document.createElement('style');
        styleEl.textContent = styles;
        document.head.appendChild(styleEl);
    }

    // ===== Markdown 简单解析（轻量版） =====
    function simpleMarkdown(text) {
        // 简易 markdown 解析
        let html = text
            // 图片（必须在链接之前处理）
            .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%;max-height:200px;border-radius:6px;margin:6px 0;" onerror="this.style.display=\'none\'">')
            // 链接
            .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
            // 代码块
            .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
            // 行内代码
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            // 加粗
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
            // 斜体
            .replace(/\*([^*]+)\*/g, '<em>$1</em>')
            // 换行
            .replace(/\n/g, '<br>');
        return html;
    }

    // ===== 工具函数 =====
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function generateSessionId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2, 4);
    }

    function generateChatTitle() {
        const ts = Date.now().toString();
        return ts.slice(ts.length - 6);
    }

    function formatTime(timestamp) {
        const d = new Date(timestamp);
        return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }

    // ===== 状态 =====
    const state = {
        chats: new Map(),
        currentSessionId: null,
        isStreaming: false,
        abortController: null,
        examples: [],
        isOpen: false,
        historySessions: {},  // 按时间分组的历史会话 { "当天": [...], "最近30天": [...] }
        historyOpen: false,   // 历史会话侧边栏是否打开
    };

    // ===== DOM 构建 =====
    let elements = {};

    function buildUI() {
        // 浮动按钮 - 可拖拽
        const floatBtn = document.createElement('button');
        floatBtn.className = 'linkai-float-btn';
        floatBtn.id = 'linkaiFloatBtn';
        floatBtn.innerHTML = '<span>🤖</span><span class="linkai-badge" id="linkaiBadge">1</span>';
        floatBtn.title = '打开 LinkAI 智能助手';

        // 初始化位置（右下角）
        let posX = window.innerWidth - 90;  // 60px 按钮 + 30px 间距
        let posY = window.innerHeight - 90;
        floatBtn.style.left = posX + 'px';
        floatBtn.style.top = posY + 'px';

        // 拖拽状态
        let isDragging = false;
        let dragStartX = 0;
        let dragStartY = 0;
        let dragStartLeft = 0;
        let dragStartTop = 0;
        let clickMoved = false;

        // 鼠标/触摸拖拽事件
        function startDrag(clientX, clientY) {
            isDragging = true;
            clickMoved = false;
            dragStartX = clientX;
            dragStartY = clientY;
            dragStartLeft = parseFloat(floatBtn.style.left) || posX;
            dragStartTop = parseFloat(floatBtn.style.top) || posY;
            floatBtn.classList.add('linkai-dragging');
        }

        function moveDrag(clientX, clientY) {
            if (!isDragging) return;
            const dx = clientX - dragStartX;
            const dy = clientY - dragStartY;
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                clickMoved = true;
            }
            let newLeft = dragStartLeft + dx;
            let newTop = dragStartTop + dy;
            // 边界限制
            newLeft = Math.max(0, Math.min(window.innerWidth - 60, newLeft));
            newTop = Math.max(0, Math.min(window.innerHeight - 60, newTop));
            floatBtn.style.left = newLeft + 'px';
            floatBtn.style.top = newTop + 'px';
            // 更新面板位置
            updatePanelPosition(newLeft, newTop);
        }

        function endDrag() {
            if (!isDragging) return;
            isDragging = false;
            floatBtn.classList.remove('linkai-dragging');
            // 更新存储的位置
            posX = parseFloat(floatBtn.style.left);
            posY = parseFloat(floatBtn.style.top);
        }

        // 鼠标事件
        floatBtn.addEventListener('mousedown', function(e) {
            startDrag(e.clientX, e.clientY);
            e.preventDefault();
        });
        document.addEventListener('mousemove', function(e) {
            moveDrag(e.clientX, e.clientY);
        });
        document.addEventListener('mouseup', function() {
            endDrag();
        });

        // 触摸事件（移动端支持）
        floatBtn.addEventListener('touchstart', function(e) {
            const touch = e.touches[0];
            startDrag(touch.clientX, touch.clientY);
            e.preventDefault();
        });
        document.addEventListener('touchmove', function(e) {
            const touch = e.touches[0];
            moveDrag(touch.clientX, touch.clientY);
        });
        document.addEventListener('touchend', function() {
            endDrag();
        });

        // 点击事件（区分拖拽和点击）
        floatBtn.addEventListener('click', function(e) {
            if (clickMoved) {
                e.stopPropagation();
                return;
            }
            togglePanel();
        });

        // 聊天面板
        const panel = document.createElement('div');
        panel.className = 'linkai-panel';
        panel.id = 'linkaiPanel';

        panel.innerHTML = `
            <div class="linkai-header">
                <div class="linkai-header-left">
                    <div class="linkai-avatar">🤖</div>
                    <div class="linkai-header-info">
                        <h3>LinkAI 助手</h3>
                        <p>智能购物助手 · 在线</p>
                    </div>
                </div>
                <div style="display:flex;align-items:center;gap:8px;">
                    <button class="linkai-header-history-btn" id="linkaiHistoryBtn" title="历史会话" style="background:none;border:none;color:#fff;font-size:18px;cursor:pointer;opacity:0.7;padding:4px;line-height:1;transition:opacity 0.2s;">📋</button>
                    <button class="linkai-header-close" id="linkaiCloseBtn">×</button>
                </div>
            </div>
            <div style="display:flex;flex:1;overflow:hidden;position:relative;">
                <div class="linkai-history-sidebar" id="linkaiHistorySidebar" style="display:none;width:200px;background:#f4f5f9;border-right:1px solid #e8e8e8;overflow-y:auto;flex-shrink:0;">
                    <div style="padding:12px 14px;font-size:13px;font-weight:600;color:#333;border-bottom:1px solid #e8e8e8;display:flex;justify-content:space-between;align-items:center;">
                        <span>历史会话</span>
                        <button id="linkaiNewChatBtn" style="background:#4f46e5;color:#fff;border:none;border-radius:6px;padding:4px 10px;font-size:12px;cursor:pointer;">+ 新建</button>
                    </div>
                    <div id="linkaiHistoryList" style="padding:8px 0;"></div>
                </div>
                <div class="linkai-messages" id="linkaiMessages">
                    <div class="linkai-msg linkai-assistant">
                        <div class="linkai-msg-avatar">🤖</div>
                        <div class="linkai-msg-bubble">${CONFIG.WELCOME_MESSAGE}</div>
                    </div>
                </div>
            </div>
            <div class="linkai-input-area">
                <div class="linkai-input-wrapper">
                    <textarea id="linkaiInput" rows="1" placeholder="输入消息，例如：帮我推荐一款手机..." 
                        onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();window.__linkaiSendMessage();}"></textarea>
                    <button class="linkai-btn-send" id="linkaiSendBtn" onclick="window.__linkaiSendMessage()" disabled>
                        ▶
                    </button>
                    <button class="linkai-btn-stop" id="linkaiStopBtn" onclick="window.__linkaiStopStream()" title="停止生成">
                        ■
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(floatBtn);
        document.body.appendChild(panel);

        // 缓存 DOM 引用
        elements = {
            floatBtn,
            panel,
            messages: panel.querySelector('#linkaiMessages'),
            input: panel.querySelector('#linkaiInput'),
            sendBtn: panel.querySelector('#linkaiSendBtn'),
            stopBtn: panel.querySelector('#linkaiStopBtn'),
            closeBtn: panel.querySelector('#linkaiCloseBtn'),
            badge: floatBtn.querySelector('.linkai-badge'),
        };

        // 事件绑定
        elements.closeBtn.addEventListener('click', togglePanel);
        elements.input.addEventListener('input', function () {
            elements.sendBtn.disabled = !this.value.trim() || state.isStreaming;
            autoResize(this);
        });

        // 历史会话按钮
        const historyBtn = panel.querySelector('#linkaiHistoryBtn');
        const historySidebar = panel.querySelector('#linkaiHistorySidebar');
        const historyList = panel.querySelector('#linkaiHistoryList');
        const newChatBtn = panel.querySelector('#linkaiNewChatBtn');
        elements.historyBtn = historyBtn;
        elements.historySidebar = historySidebar;
        elements.historyList = historyList;
        elements.newChatBtn = newChatBtn;

        historyBtn.addEventListener('click', function() {
            state.historyOpen = !state.historyOpen;
            historySidebar.style.display = state.historyOpen ? 'block' : 'none';
            if (state.historyOpen) {
                loadHistorySessions();
            }
        });

        newChatBtn.addEventListener('click', function() {
            // 新建会话：清空当前会话，关闭历史侧边栏
            state.currentSessionId = null;
            state.historyOpen = false;
            historySidebar.style.display = 'none';
            // 清空消息区域，显示欢迎消息
            elements.messages.innerHTML = `
                <div class="linkai-msg linkai-assistant">
                    <div class="linkai-msg-avatar">🤖</div>
                    <div class="linkai-msg-bubble">${CONFIG.WELCOME_MESSAGE}</div>
                </div>
            `;
            elements.input.focus();
        });

        // 暴露全局函数供内联事件使用
        window.__linkaiSendMessage = sendMessage;
        window.__linkaiStopStream = stopStream;

        // 加载示例问题
        loadExamples();
    }

    function autoResize(textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 80) + 'px';
    }

    function scrollToBottom() {
        setTimeout(() => {
            elements.messages.scrollTop = elements.messages.scrollHeight;
        }, 50);
    }

    // ===== 更新面板位置（面板在按钮上方）=====
    function updatePanelPosition(btnLeft, btnTop) {
        const panel = document.getElementById('linkaiPanel');
        if (!panel) return;
        const panelWidth = 380;
        const panelHeight = 600;
        let panelLeft = btnLeft + 60 - panelWidth;
        let panelTop = btnTop - panelHeight - 10;
        // 边界限制
        if (panelLeft < 5) panelLeft = 5;
        if (panelLeft + panelWidth > window.innerWidth - 5) {
            panelLeft = window.innerWidth - panelWidth - 5;
        }
        if (panelTop < 5) panelTop = btnTop + 70; // 面板在按钮下方
        panel.style.left = panelLeft + 'px';
        panel.style.top = panelTop + 'px';
    }

    // ===== 面板开关 =====
    function togglePanel() {
        state.isOpen = !state.isOpen;
        elements.panel.classList.toggle('linkai-show', state.isOpen);
        elements.floatBtn.classList.toggle('linkai-hidden', state.isOpen);
        if (state.isOpen) {
            // 打开时更新面板位置
            const btnLeft = parseFloat(elements.floatBtn.style.left);
            const btnTop = parseFloat(elements.floatBtn.style.top);
            updatePanelPosition(btnLeft, btnTop);
            elements.input.focus();
            scrollToBottom();
        }
    }

    // ===== 示例问题 =====
    async function loadExamples() {
        try {
            const response = await fetch(CONFIG.API_BASE + CONFIG.SESSION_ENDPOINT + '/hot?n=3', {
                method: 'GET',
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const data = await response.json();
            state.examples = data || [];
        } catch (err) {
            console.error('[LinkAI] 加载示例问题失败:', err);
        }
    }

    function renderExamples(examples) {
        if (!examples || examples.length === 0) return '';
        const icons = ['📱', '👕', '💻', '🎧', '⌚', '📷', '🖥️', '🏷️'];
        return `
            <div class="linkai-examples">
                ${examples.slice(0, 3).map((ex, i) => `
                    <div class="linkai-example-item" onclick="window.__linkaiUseExample('${escapeHtml(ex.description)}')">
                        <span class="linkai-example-icon">${icons[i % icons.length]}</span>
                        ${escapeHtml(ex.description)}
                    </div>
                `).join('')}
            </div>
        `;
    }

    // 暴露示例点击
    window.__linkaiUseExample = function (text) {
        elements.input.value = text;
        elements.sendBtn.disabled = false;
        sendMessage();
    };

    // ===== 发送消息 =====
    async function sendMessage() {
        const text = elements.input.value.trim();
        if (!text || state.isStreaming) return;

        // 创建或获取当前会话
        if (!state.currentSessionId) {
            // 调用后端 API 创建会话，获取后端生成的 sessionId
            try {
                const token = sessionStorage.getItem('token') || '';
                const userInfoStr = sessionStorage.getItem('user-info');
                let userId = null;
                if (userInfoStr) {
                    try {
                        const userInfo = JSON.parse(userInfoStr);
                        userId = userInfo.userId || null;
                    } catch (e) {}
                }

                const headers = { 'Content-Type': 'application/json' };
                if (token) {
                    headers['authorization'] = token;
                }
                let url = CONFIG.API_BASE + CONFIG.SESSION_ENDPOINT + '?n=3';
                if (userId) {
                    url += '&userId=' + userId;
                }
                const sessionResp = await fetch(url, {
                    method: 'POST',
                    headers: headers,
                });
                if (sessionResp.ok) {
                    const sessionData = await sessionResp.json();
                    const sessionId = sessionData.sessionId;
                    const title = generateChatTitle();
                    state.chats.set(sessionId, {
                        title: title,
                        messages: [],
                        timestamp: Date.now(),
                    });
                    state.currentSessionId = sessionId;
                } else {
                    throw new Error(`HTTP ${sessionResp.status}`);
                }
            } catch (err) {
                console.error('[LinkAI] 创建会话失败:', err);
                // 降级方案：前端自己生成 sessionId
                const sessionId = generateSessionId();
                const title = generateChatTitle();
                state.chats.set(sessionId, {
                    title: title,
                    messages: [],
                    timestamp: Date.now(),
                });
                state.currentSessionId = sessionId;
            }
        }

        const sessionId = state.currentSessionId;

        // 添加用户消息
        appendMessage('user', text);

        // 清空输入
        elements.input.value = '';
        elements.sendBtn.disabled = true;

        // 添加占位 assistant 消息
        const assistantBubble = appendAssistantMessage('', true);

        // 准备流式接收
        state.isStreaming = true;
        state.abortController = new AbortController();
        elements.sendBtn.style.display = 'none';
        elements.stopBtn.style.display = 'flex';

        let fullContent = '';

        try {
            // 从主应用的 sessionStorage 获取 token 和 userId
            const token = sessionStorage.getItem('token') || '';
            const userInfoStr = sessionStorage.getItem('user-info');
            let userId = null;
            if (userInfoStr) {
                try {
                    const userInfo = JSON.parse(userInfoStr);
                    userId = userInfo.userId || null;
                } catch (e) {}
            }

            console.log('[LinkAI] 发送请求 - token:', token ? token.substring(0, 20) + '...' : '无');
            console.log('[LinkAI] 发送请求 - userId:', userId);

            const headers = { 'Content-Type': 'application/json' };
            if (token) {
                headers['authorization'] = token;
            }

            const response = await fetch(CONFIG.API_BASE + CONFIG.CHAT_ENDPOINT, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    question: text,
                    sessionId: sessionId,
                    userId: userId,
                }),
                signal: state.abortController.signal,
            });

            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        try {
                            const jsonStr = line.substring(5).trim();
                            const event = JSON.parse(jsonStr);

                            if (event.eventType === 1001) {
                                // 文本数据
                                fullContent += event.eventData || '';
                                assistantBubble.innerHTML = simpleMarkdown(fullContent) + '<span class="linkai-streaming-cursor"></span>';
                                scrollToBottom();
                            } else if (event.eventType === 1002) {
                                // 流结束
                                console.log('[LinkAI] Stream completed');
                            } else if (event.eventType === 1003) {
                                // 商品数据
                                const itemData = event.eventData;
                                console.log('[LinkAI] 收到商品数据:', itemData);
                                let productCards = '';
                                for (const [key, item] of Object.entries(itemData)) {
                                    if (item && item.image) {
                                        // 价格从分转换为元
                                        let price = parseFloat(item.price) || 0;
                                        price = price > 100 ? (price / 100).toFixed(2) : price.toFixed(2);
                                        // 规格过滤空JSON
                                        let spec = item.spec || '';
                                        if (spec === '{}' || spec === '[]') spec = '';
                                        // 清理图片URL中可能的尾部乱码
                                        let imgUrl = item.image || '';

                                        productCards += `
                                            <div class="linkai-product-card">
                                                <div class="linkai-product-card-inner">
                                                    <div class="linkai-product-name">${item.name || '商品名称'}</div>
                                                    <div class="linkai-product-price">¥${price}</div>
                                                    <div class="linkai-product-meta">${item.category || ''} ${item.brand ? '| ' + item.brand : ''}</div>
                                                    ${spec ? `<div class="linkai-product-meta">${spec}</div>` : ''}
                                                    <div class="linkai-product-img-wrapper">
                                                        <img src="${imgUrl}" alt="${item.name || '商品图片'}" 
                                                             onerror="this.alt='图片加载失败';this.style.background='#f5f5f5';this.style.padding='40px 0';this.style.fontSize='12px'">
                                                    </div>
                                                </div>
                                            </div>
                                        `;
                                    }
                                }
                                if (productCards) {
                                    const cardsContainer = document.createElement('div');
                                    cardsContainer.className = 'linkai-product-cards';
                                    cardsContainer.innerHTML = productCards;
                                    // 插入到整个消息条目的后面，而不是气泡内部
                                    const msgDiv = assistantBubble.parentNode;
                                    msgDiv.parentNode.insertBefore(cardsContainer, msgDiv.nextSibling);
                                    scrollToBottom();
                                }
                            } else if (event.eventType === 1004) {
                                // 购物车数据
                                console.log('[LinkAI] 购物车数据:', event.eventData);
                                alert('商品已成功加入购物车！');
                            }
                        } catch (e) {
                            // 非 JSON 行跳过
                        }
                    }
                }
            }

            assistantBubble.classList.remove('linkai-streaming-cursor');
            assistantBubble.innerHTML = simpleMarkdown(fullContent);

        } catch (err) {
            if (err.name === 'AbortError') {
                assistantBubble.innerHTML = fullContent
                    ? simpleMarkdown(fullContent) + '<br><br><span style="color:#999;">⏹️ 已停止生成</span>'
                    : '<span style="color:#999;">⏹️ 已停止生成</span>';
            } else {
                assistantBubble.innerHTML = '❌ 出错了: ' + escapeHtml(err.message);
                console.error('[LinkAI] Stream error:', err);
            }
        } finally {
            state.isStreaming = false;
            state.abortController = null;
            elements.sendBtn.style.display = 'flex';
            elements.stopBtn.style.display = 'none';
            elements.sendBtn.disabled = false;
            elements.input.focus();
        }
    }

    // ===== 停止生成 =====
    async function stopStream() {
        if (!state.currentSessionId || !state.isStreaming) return;
        const sessionId = state.currentSessionId;

        try {
            if (state.abortController) {
                state.abortController.abort();
            }
            await fetch(CONFIG.API_BASE + CONFIG.STOP_ENDPOINT + '?sessionId=' + encodeURIComponent(sessionId), {
                method: 'POST',
            });
        } catch (err) {
            console.error('[LinkAI] 停止请求失败:', err);
        }
    }

    // ===== 历史会话 =====
    async function loadHistorySessions() {
        try {
            const token = sessionStorage.getItem('token') || '';
            const headers = {};
            if (token) {
                headers['authorization'] = token;
            }
            const response = await fetch(CONFIG.API_BASE + CONFIG.SESSION_ENDPOINT + '/history', {
                method: 'GET',
                headers: headers,
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const data = await response.json();
            state.historySessions = data || {};
            renderHistorySessions();
        } catch (err) {
            console.error('[LinkAI] 加载历史会话失败:', err);
        }
    }

    function renderHistorySessions() {
        const list = elements.historyList;
        if (!list) return;

        const groups = state.historySessions;
        const groupKeys = Object.keys(groups);

        if (groupKeys.length === 0) {
            list.innerHTML = '<div style="padding:20px;text-align:center;color:#999;font-size:12px;">暂无历史会话</div>';
            return;
        }

        let html = '';
        // 按优先级排序分组
        const order = ['当天', '最近30天', '最近1年', '1年以上'];
        for (const key of order) {
            const sessions = groups[key];
            if (!sessions || sessions.length === 0) continue;

            html += `<div style="padding:4px 14px 2px;font-size:11px;color:#999;font-weight:500;">${key}</div>`;

            for (const session of sessions) {
                const title = session.title || '新会话';
                const time = session.updateTime ? formatTime(new Date(session.updateTime).getTime()) : '';
                const isActive = state.currentSessionId === session.sessionId;
                html += `
                    <div class="linkai-history-item" data-session-id="${session.sessionId}" 
                         style="padding:8px 14px;cursor:pointer;display:flex;align-items:center;justify-content:space-between;gap:6px;
                                background:${isActive ? '#e8e7ff' : 'transparent'};
                                border-left:${isActive ? '3px solid #4f46e5' : '3px solid transparent'};
                                transition:background 0.2s;"
                         onmouseover="this.style.background='${isActive ? '#e8e7ff' : '#eee'}'"
                         onmouseout="this.style.background='${isActive ? '#e8e7ff' : 'transparent'}'">
                        <div style="flex:1;overflow:hidden;font-size:12px;color:#333;white-space:nowrap;text-overflow:ellipsis;" 
                             onclick="window.__linkaiSwitchSession('${session.sessionId}')">
                            ${escapeHtml(title)}
                            ${time ? `<span style="font-size:10px;color:#999;margin-left:4px;">${time}</span>` : ''}
                        </div>
                        <button onclick="event.stopPropagation();window.__linkaiDeleteSession('${session.sessionId}')" 
                                style="background:none;border:none;cursor:pointer;color:#ccc;font-size:12px;padding:2px;flex-shrink:0;"
                                onmouseover="this.style.color='#ef4444'" onmouseout="this.style.color='#ccc'"
                                title="删除">✕</button>
                    </div>
                `;
            }
        }

        list.innerHTML = html;
    }

    // 切换到历史会话
    window.__linkaiSwitchSession = async function(sessionId) {
        try {
            const response = await fetch(CONFIG.API_BASE + CONFIG.SESSION_ENDPOINT + '/' + encodeURIComponent(sessionId), {
                method: 'GET',
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const messages = await response.json();

            // 清空消息区域
            elements.messages.innerHTML = '';

            // 渲染消息
            if (messages && messages.length > 0) {
                for (const msg of messages) {
                    if (msg.type === 'USER' || msg.type === 'user') {
                        appendMessage('user', msg.content || '');
                    } else if (msg.type === 'ASSISTANT' || msg.type === 'assistant') {
                        appendAssistantMessage(msg.content || '', false);
                    }
                }
            }

            state.currentSessionId = sessionId;

            // 更新历史列表高亮
            renderHistorySessions();

            scrollToBottom();
        } catch (err) {
            console.error('[LinkAI] 加载会话消息失败:', err);
        }
    };

    // 删除历史会话
    window.__linkaiDeleteSession = async function(sessionId) {
        if (!confirm('确定要删除该会话吗？')) return;

        try {
            const token = sessionStorage.getItem('token') || '';
            const headers = {};
            if (token) {
                headers['authorization'] = token;
            }
            const response = await fetch(CONFIG.API_BASE + CONFIG.SESSION_ENDPOINT + '/history?sessionId=' + encodeURIComponent(sessionId), {
                method: 'DELETE',
                headers: headers,
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            // 如果删除的是当前会话，清空当前会话
            if (state.currentSessionId === sessionId) {
                state.currentSessionId = null;
                elements.messages.innerHTML = `
                    <div class="linkai-msg linkai-assistant">
                        <div class="linkai-msg-avatar">🤖</div>
                        <div class="linkai-msg-bubble">${CONFIG.WELCOME_MESSAGE}</div>
                    </div>
                `;
            }

            // 重新加载历史列表
            loadHistorySessions();
        } catch (err) {
            console.error('[LinkAI] 删除会话失败:', err);
        }
    };

    // ===== 消息渲染 =====
    function appendMessage(role, content) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'linkai-msg linkai-' + role;

        const avatar = document.createElement('div');
        avatar.className = 'linkai-msg-avatar';
        avatar.textContent = role === 'user' ? '👤' : '🤖';

        const bubble = document.createElement('div');
        bubble.className = 'linkai-msg-bubble';

        if (role === 'user') {
            bubble.textContent = content;
        } else {
            bubble.innerHTML = simpleMarkdown(content);
        }

        msgDiv.appendChild(avatar);
        msgDiv.appendChild(bubble);
        elements.messages.appendChild(msgDiv);
        scrollToBottom();

        return bubble;
    }

    function appendAssistantMessage(content, isStreaming) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'linkai-msg linkai-assistant';

        const avatar = document.createElement('div');
        avatar.className = 'linkai-msg-avatar';
        avatar.textContent = '🤖';

        const bubble = document.createElement('div');
        bubble.className = 'linkai-msg-bubble';
        if (isStreaming) {
            bubble.classList.add('linkai-streaming-cursor');
        }
        bubble.innerHTML = content ? simpleMarkdown(content) : '';

        msgDiv.appendChild(avatar);
        msgDiv.appendChild(bubble);
        elements.messages.appendChild(msgDiv);
        scrollToBottom();

        return bubble;
    }

    // ===== 初始化 =====
    function init() {
        injectStyles();

        // 等待 DOM 加载完成
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', buildUI);
        } else {
            buildUI();
        }
    }

    init();

})();