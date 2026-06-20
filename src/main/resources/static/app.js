// Получаем окно miniApp
const tg = window.Telegram.WebApp;
tg.expand(); // Расширяем

// Глобальные поля
let globalInstruments = []; // Список всех инструментов для выбора (тянем с сервака)
let selectedInstrumentIds = new Set(); // Выбранные инструменты в заполнении / редактировании анкеты
let selectedFilterInstrumentIds = new Set(); // Выбранные инструменты в окне поиска jam buddy // todo: удалить?? Вроде как можно не использовать\
let userLongitude = null; // Координаты текущего юзера
let userLatitude = null;
let searchResults = []; // Результат поиска по анкетам для отрисовки в списке и на карте
let incomingInvites = []; // Входящие
let outgoingInvites = []; // И исходящие приглашения
let currentTab = 'incoming'; // Значение вкладки окна приглашений. По умолчанию - входящие
let currentProfile = null; // Объект анкеты текущего пользователя (если создана / или скачана с сервера)

// TODO:
// - Приглашения: + Выход в главное меню. + Сделать навигацию и что-то с самим заголовком.
// - Результаты поиска: + Навигация в главное меню.
// - Карта: + Навигация в главное меню. При обновлении перерисовывать не карту, а текущую карточку.


// Аутентификация
async function authenticate() {
    const initData = tg.initData;
    if (!initData) {
        console.error("WebApp.initData пуст.");
        return;
    }

    try {
        const response = await fetch("/auth/telegram", {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: initData
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem("jwt", data.accessToken);
            loadProfile();
        } else {
            console.error("Ошибка авторизации:", response.status);
        }
    } catch (e) {
        console.error("Сетевая ошибка:", e);
    }
}
// При запуске приложения сразу отсылаем на сервер данные для входа в систему
authenticate();

// Загрузка профиля (анкеты музыканта текущего пользователя)
async function loadProfile() {

    if(currentProfile === null){
        try {
            const response = await fetch(`/profiles/my`, {
                headers: { "Authorization": "Bearer " + localStorage.getItem("jwt") }
            });

            if (response.status === 200) {
                const data = await response.json();
                if (data && data.profile) {
                    userLatitude = data.profile.latitude;
                    userLongitude = data.profile.longitude;
                    currentProfile = data.profile;
                    loadInvites();
                    subscribeToInviteEvents();
                    renderMainScreen();
                    return;
                }
            }
            renderProfileForm();
        } catch (e) {
            renderProfileForm();
        }
    }
    renderMainScreen();
}

// Загружаем данные о приглашениях
async function loadInvites(){

    try{
        const response = await fetch(`/profiles/invite/incoming`, {
            headers: { "Authorization": "Bearer " + localStorage.getItem("jwt") }
        });
        if (response.ok) {
            const data = await response.json();
            incomingInvites = data.list;
        }
    } catch (e){ console.error("Ошибка загрузки входящих приглашений")}

    try{
        const response = await fetch(`/profiles/invite/outgoing`, {
            headers: { "Authorization": "Bearer " + localStorage.getItem("jwt") }
        });
        if (response.ok) {
            const data = await response.json();
            outgoingInvites = data.list;
        }
    } catch (e){ console.error("Ошибка загрузки исходящих приглашений")}

}

// Отрисовка формы заполнения анкеты (при первичном заполнении так и при редактировании)
async function renderProfileForm(existingProfile = null) {
    const isEdit = !!existingProfile;
    document.getElementById("title").innerText = isEdit ? "Редактирование анкеты" : "Создание анкеты";

    selectedInstrumentIds.clear();

    try {
        const res = await fetch("/profiles/instruments", {
            headers: { "Authorization": "Bearer " + localStorage.getItem("jwt") }
        });
        const data = await res.json();
        globalInstruments = data.instruments || [];
    } catch (e) {
        console.error("Ошибка при получении инструментов", e);
    }

    if (isEdit && existingProfile.instruments) {
        existingProfile.instruments.forEach(ins => {
            selectedInstrumentIds.add(ins.id.toString());
        });
    }

    document.getElementById("content").innerHTML = `
        <div class="form-container" style="padding: 15px;">
            <div class="input-group">
                <label>Имя</label>
                <input id="p-name" type="text" class="input-field" value="${existingProfile?.name || ''}" />
            </div>

            <div class="input-row" style="display: flex; gap: 10px;">
                <div style="flex: 1;">
                    <label>Возраст</label>
                    <input id="p-age" type="number" class="input-field" style="width: 100%;" value="${existingProfile?.age || ''}" />
                </div>
                <div style="flex: 1;">
                    <label>Пол</label>
                    <select id="p-gender" class="input-field" style="width: 100%;">
                        <option value="MALE" ${existingProfile?.gender === 'MALE' ? 'selected' : ''}>Мужской</option>
                        <option value="FEMALE" ${existingProfile?.gender === 'FEMALE' ? 'selected' : ''}>Женский</option>
                    </select>
                </div>
            </div>

            <div class="input-group">
                <label>О себе</label>
                <textarea id="p-description" rows="3" class="input-field">${existingProfile?.description || ''}</textarea>
            </div>

            <div class="input-group">
                <label>Инструменты</label>
                <div id="selected-chips" class="chips-container"></div>
                <select id="ins-select" class="input-field"></select>
            </div>

            <div id="geo-status" class="geo-badge" style="padding: 10px; text-align: center; border-radius: 8px; background: #eee; margin: 10px 0;">
                ${isEdit ? "📍 Локация сохранена (нажми для обновления)" : "🔍 Поиск местоположения..."}
            </div>
            <input type="hidden" id="p-lat" value="${existingProfile?.latitude || 0}" />
            <input type="hidden" id="p-lon" value="${existingProfile?.longitude || 0}" />

            <button id="save-profile-btn" class="save-btn" onclick="submitProfile()"
                style="width: 100%; padding: 15px; background: #31b545; color: white; border: none; border-radius: 10px; font-weight: bold;">
                ${isEdit ? "Обновить анкету" : "Сохранить анкету"}
            </button>

            ${isEdit ? `<button class="save-btn" style="width:100%; background: #666; margin-top: 10px;" onclick="renderMainScreen()">Отмена</button>` : ''}
        </div>
    `;

    if (isEdit) {
        const container = document.getElementById("selected-chips");
        existingProfile.instruments.forEach(ins => {
            const chip = document.createElement("div");
            chip.className = "chip";
            chip.id = `chip-${ins.id}`;
            chip.innerHTML = `${ins.name} <span class="remove-btn" onclick="removeInstrument('${ins.id}')">×</span>`;
            container.appendChild(chip);
        });
    }

    const select = document.getElementById("ins-select");
    refreshSelectOptions(select, globalInstruments);
    initInstrumentLogic(select, globalInstruments);

    if (isEdit) {
        const status = document.getElementById("geo-status");
        status.style.background = "#e8f5e9";
        status.style.color = "#2e7d32";
        status.style.cursor = "pointer";
        status.onclick = () => getHybridLocation();
    } else {
        getHybridLocation();
    }
}

// Получаем данные о геолокации (Гибридный метод: пробуем сперва инструментарий телеги, затем функционал браузера)
function getHybridLocation() {
    const status = document.getElementById("geo-status");
    const saveBtn = document.getElementById("save-profile-btn");

    console.log("Запуск процесса получения ГЕО...");

    if (status) {
        status.innerText = "⏳ Определение локации...";
        status.style.background = "#fff3e0";
        status.style.color = "#e65100";
    }

    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.style.opacity = "0.5";
        saveBtn.innerText = "Ожидайте ГЕО...";
    }

    if (typeof tg.getLocation === 'function') {
        tg.getLocation((data) => {
            if (data && data.latitude) {
                applyCoords(data.latitude, data.longitude, "📍 Локация обновлена (TG)");
            } else {
                fallbackToBrowserGeo();
            }
        });
    } else {
        fallbackToBrowserGeo();
    }
}

// Если через инструментарий телеграм не вышло получить гео, делаем через функционал браузера
function fallbackToBrowserGeo() {
    const status = document.getElementById("geo-status");

    if (!navigator.geolocation) {
        status.innerText = "⚠️ Браузер не поддерживает ГЕО";
        return;
    }

    const options = {
        enableHighAccuracy: false,
        timeout: 30000,
        maximumAge: 0
    };

    console.log("Запрос navigator.geolocation...");
    navigator.geolocation.getCurrentPosition(
        (pos) => {
            console.log("ГЕО получено через Браузер:", pos.coords);
            applyCoords(pos.coords.latitude, pos.coords.longitude, "📍 Локация определена (Браузер)");
        },
        (err) => {
            console.error("Ошибка браузерного ГЕО:", err.code, err.message);
            let msg = "❌ Ошибка гео: ";
            switch(err.code) {
                case 1: msg += "Доступ запрещен"; break;
                case 2: msg += "Положение недоступно"; break;
                case 3: msg += "Таймаут"; break;
                default: msg += "Неизвестная ошибка";
            }
            status.innerText = msg;
            status.style.background = "#ffebee";
        },
        options
    );
}

// Сохраняем полученные данные о локации в соответствующие поля формы
function applyCoords(lat, lon, text) {
    const latInp = document.getElementById("p-lat");
    const lonInp = document.getElementById("p-lon");
    const status = document.getElementById("geo-status");
    const saveBtn = document.getElementById("save-profile-btn");

    if (latInp && lonInp) {
        latInp.value = lat;
        lonInp.value = lon;
    }

    if (status) {
        status.innerText = text;
        status.style.background = "#e8f5e9";
        status.style.color = "#2e7d32";
        status.style.cursor = "pointer";
        status.style.border = "1px solid #2e7d32";

        status.onclick = () => openLocationInMap(lat, lon);
    }

    if (saveBtn) {
        saveBtn.disabled = false;
        saveBtn.style.opacity = "1";
        saveBtn.style.background = "#31b545";
        saveBtn.innerText = "Сохранить";
    }
}

// Функция открытия карты на указанных координатах
function openLocationInMap(lat, lon) {
    const url = `https://yandex.ru/maps/?pt=${lon},${lat}&z=15&l=map`;
    tg.openLink(url);
}


// Включаем логику работы с набором музыкальных инструментов в форме профиля (анкеты музыканта)
function initInstrumentLogic(select, instrumentsList) {
    select.addEventListener("change", (e) => {
        const id = e.target.value;
        if (!id) return;
        const option = e.target.options[e.target.selectedIndex];
        const name = option.getAttribute('data-pure-name');

        if (selectedInstrumentIds.has(id)) {
            removeInstrument(id);
        } else {
            addInstrument(id, name);
        }
        refreshSelectOptions(select, instrumentsList);
        select.value = "";
    });
}

// Добавить музыкальный инструмент в форму профиля
function addInstrument(id, name) {
    selectedInstrumentIds.add(id);
    const container = document.getElementById("selected-chips");
    const chip = document.createElement("div");
    chip.className = "chip";
    chip.id = `chip-${id}`;
    chip.innerHTML = `${name} <span class="remove-btn" onclick="removeInstrument('${id}')">×</span>`;
    container.appendChild(chip);
}

// Убрать музыкальный инструмент из формы профиля
function removeInstrument(id) {
    selectedInstrumentIds.delete(id);
    const chip = document.getElementById(`chip-${id}`);
    if (chip) chip.remove();
    const select = document.getElementById("ins-select");
    if (select) refreshSelectOptions(select, globalInstruments);
}

// Обновляем список выпадающего меню музыкальных инструментов с учетом текущего выбора
function refreshSelectOptions(select, instrumentsList) {
    let html = `<option value="">-- Выберите инструмент --</option>`;
    instrumentsList.forEach(i => {
        const isSelected = selectedInstrumentIds.has(i.id.toString());
        const icon = isSelected ? "—" : "+";
        html += `<option value="${i.id}" data-pure-name="${i.name}" ${isSelected ? 'style="color: #ff4d4d; font-weight: bold;"' : ''}>
                    ${i.name} (${icon})
                 </option>`;
    });
    select.innerHTML = html;
}

// Выслать форму профиля (анкеты музыканта) на сервер
async function submitProfile() {
    const saveBtn = document.getElementById("save-profile-btn");
    const name = document.getElementById("p-name").value;

    if(!name) {
        tg.showAlert("Введите имя!");
        return;
    }

    saveBtn.disabled = true;
    const originalBtnText = saveBtn.innerText;
    saveBtn.innerText = "⌛ Сохранение...";
    saveBtn.style.opacity = "0.7";
    saveBtn.style.cursor = "not-allowed";

    const payload = {
        name: name,
        age: parseInt(document.getElementById("p-age").value) || 0,
        gender: document.getElementById("p-gender").value,
        description: document.getElementById("p-description").value,
        latitude: parseFloat(document.getElementById("p-lat").value),
        longitude: parseFloat(document.getElementById("p-lon").value),
        instruments: Array.from(selectedInstrumentIds).map(Number),
        telegramUsername: tg.initDataUnsafe.user.username
    };

    try {
        const res = await fetch("/profiles", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("jwt")
            },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            tg.showAlert("Анкета успешно сохранена!");
            loadProfile();
        } else {
            tg.showAlert("Ошибка сохранения: " + res.status);
            resetSaveButton(saveBtn, originalBtnText);
        }
    } catch (e) {
        console.error("Ошибка POST профиля:", e);
        tg.showAlert("Сетевая ошибка. Попробуйте позже.");
        resetSaveButton(saveBtn, originalBtnText);
    }
}

function resetSaveButton(btn, text) {
    btn.disabled = false;
    btn.innerText = text;
    btn.style.opacity = "1";
    btn.style.cursor = "pointer";
    btn.style.background = "#31b545";
}

// Отрисовка главного меню приложения
function renderMainScreen() {
    document.getElementById("title").innerText = "Главное меню";
    document.getElementById("content").innerHTML = `
        <div style="padding: 15px; text-align: center;">
            <p>Привет, <b>${currentProfile.name}</b>!</p>
            <button class="save-btn" style="width:100%; margin-bottom: 10px;" onclick="renderSearchFilters()">🔍 Найти музыкантов рядом</button>
            <button class="save-btn" style="width:100%; background: #0088cc" onclick='renderProfileForm(${JSON.stringify(currentProfile)})'>⚙️ Моя анкета</button>
            <button class="save-btn" style="width:100%; background: #0088cc" onclick='renderInvitesScreen()'> Мои приглашения </button>
        </div>
    `;
}

// Отрисовка формы с фильтрами для поиска музыкантов
async function renderSearchFilters() {

    document.getElementById("title").innerText = "Настройка поиска";

    selectedInstrumentIds.clear();

    // TODO: вынести в отдельную функцию и вызывать её
    // Скачиваем актуальный список инструментов
    try {
        const res = await fetch("/profiles/instruments", {
            headers: { "Authorization": "Bearer " + localStorage.getItem("jwt") }
        });
        const data = await res.json();
        globalInstruments = data.instruments || [];
    } catch (e) {
        console.error("Ошибка при получении инструментов", e);
    }

    document.getElementById("content").innerHTML = `
        <div class="form-container" style="padding: 15px;">
            <div class="input-group">
                <label>Радиус (км): <span id="radius-val" style="font-weight:bold;">50</span></label>
                <input type="range" id="f-radius-range" min="1" max="500" value="50" oninput="syncRadius(this.value, 'range')" style="width:100%;">
                <input type="number" id="f-radius-num" value="50" oninput="syncRadius(this.value, 'num')" class="input-field" style="width:100%; margin-top:5px;">
            </div>

            <div class="input-row" style="display: flex; gap: 10px; margin-top: 10px;">
                <div style="flex: 1;">
                    <label>Возраст от</label>
                    <input id="f-age-from" type="number" class="input-field" style="width:100%;">
                </div>
                <div style="flex: 1;">
                    <label>до</label>
                    <input id="f-age-to" type="number" class="input-field" style="width:100%;">
                </div>
            </div>

            <div class="input-group" style="margin-top: 10px;">
                <label>Пол</label>
                <select id="f-gender" class="input-field">
                    <option value="">Любой</option>
                    <option value="MALE">Мужской</option>
                    <option value="FEMALE">Женский</option>
                </select>
            </div>

            <div class="input-group" style="margin-top: 10px;">
                <label>Инструменты</label>
                <div id="selected-chips" class="chips-container" style="margin-bottom: 5px;"></div>
                <select id="filter-ins-select" class="input-field"></select>
            </div>

            <button class="save-btn" onclick="executeSearch()" style="width: 100%; margin-top: 20px; background: #31b545; color: white; border: none; padding: 15px; border-radius: 10px; font-weight: bold;">🔍 Искать</button>
            <button class="save-btn" style="width:100%; background: #666; margin-top: 10px; color: white; border: none; padding: 15px; border-radius: 10px;" onclick="renderMainScreen()">⬅️ Назад</button>
        </div>
    `;

    const select = document.getElementById("filter-ins-select");

    refreshSelectOptions(select, globalInstruments);

    initInstrumentLogic(select, globalInstruments);
}

window.syncRadius = (val, source) => {
    document.getElementById('radius-val').innerText = val;
    if (source === 'range') document.getElementById('f-radius-num').value = val;
    else document.getElementById('f-radius-range').value = val;
};

window.addFilterIns = (id) => {
    if (!id) return;
    selectedFilterInstrumentIds.add(id);
    updateFilterChips();
    document.getElementById("filter-ins-select").value = "";
};

function updateFilterChips() {
    const container = document.getElementById("filter-chips");
    if (!container) return;
    container.innerHTML = Array.from(selectedFilterInstrumentIds).map(id => {
        const ins = globalInstruments.find(i => i.id == id);
        return `<span class="chip" style="background:#ddd; padding:5px; margin:2px; border-radius:5px; display:inline-block;">${ins.name} <span style="cursor:pointer;" onclick="removeFilterIns('${id}')">×</span></span>`;
    }).join('');
}

window.removeFilterIns = (id) => {
    selectedFilterInstrumentIds.delete(id);
    updateFilterChips();
};

window.removeFilterIns = (id) => {
    selectedFilterInstrumentIds.delete(id);
    updateFilterChips();
};

function removeFilterIns(id) {
    selectedFilterInstrumentIds.delete(id);
    updateFilterChips();
}

// выполнить поиск (передаем форму поиска на сервер) и получить результаты
async function executeSearch() {

const instrumentsArray = Array.from(selectedInstrumentIds).map(Number);

    const payload = {
        latitude: userLatitude,
        longitude: userLongitude,
        radius: parseFloat(document.getElementById('f-radius-num').value),
        gender: document.getElementById('f-gender').value || null,
        minAge: parseInt(document.getElementById('f-age-from').value) || null,
        maxAge: parseInt(document.getElementById('f-age-to').value) || null,
        instrumentIds: instrumentsArray
    };

    const res = await fetch("/profiles/search", {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": "Bearer " + localStorage.getItem("jwt") },
        body: JSON.stringify(payload)
    });

    const data = await res.json();
    searchResults = data.profiles;
    renderSearchResults();
}

// Отображаем результаты поиска музыкантов
function renderSearchResults() {

    document.getElementById("title").innerText = "Музыканты рядом";
    const content = document.getElementById("content");

    if (!searchResults || searchResults.length === 0) {
        content.innerHTML = `
            <p id="search-result" style="text-align:center;">Никого не найдено :(</p>
            <button onclick="renderSearchFilters()" style="width:100%;">Назад к поиску</button>
        `;
        return;
    }

    let html = `
    <div id="search-result"></div>
        <button onclick="renderMap(searchResults)" style="width:100%; margin-bottom:15px; background:#0088cc; color:white; padding: 10px; border-radius: 8px; border: none; font-weight: bold;">
            🌍 Показать на карте (${searchResults.length})
        </button>

    `;

    html += searchResults.map(m => {

        const hasOutgoing = (outgoingInvites || []).some(inv => inv.receiverId === m.id);
        const hasIncoming = (incomingInvites || []).some(inv => inv.senderId === m.id);
        const isLinked = hasOutgoing || hasIncoming;

        let actionButton = '';
        if (isLinked) {
            const statusText = hasOutgoing ? "Запрос отправлен" : "Прислал вам запрос";
            actionButton = `
                <button disabled style="width:100%; margin-top:10px; background:#ccc; color:white; border:none; padding:8px; border-radius:5px;">
                    ⏳ ${statusText}
                </button>`;
        } else {
            actionButton = `
                <button onclick="sendInvite('${m.id}')" id="btn-${m.id}" style="width:100%; margin-top:10px; background:#0088cc; color:white; border:none; padding:8px; border-radius:5px; cursor:pointer;">
                    💬 Отправить приглашение
                </button>`;
        }

        return `
            <div class="profile-card" style="border:1px solid #ddd; padding:15px; margin-bottom:10px; border-radius:10px; background: white;">
                <h3 style="margin-top:0;">${m.name}, ${m.age}</h3>
                <p style="font-size:14px; color:#444;">${m.description || 'Нет описания'}</p>
                <div style="margin-bottom:8px;">
                    ${m.instruments ? m.instruments.map(ins =>
                        `<span style="background:#eee; padding:2px 8px; border-radius:10px; font-size:11px; margin-right:4px;">${ins.name}</span>`
                    ).join('') : ''}
                </div>
                <small style="color:#888;">📍 ${m.distance ? m.distance.toFixed(1) : 0} км от вас</small>
                ${actionButton}
            </div>
        `;
    }).join('');

    html += `
        <button class="save-btn" style="width:100%; background: #666; margin-top: 10px; color: white; border: none; padding: 15px; border-radius: 10px;" onclick="renderMainScreen()">
            ⬅️ Назад
        </button>
    `;

    content.innerHTML = html;
}

// Отрисовка карты с точками - музыкантами, полученными в результате поиска по фильтрам
async function renderMap() {
    document.getElementById("title").innerText = "Музыканты на карте";

    document.getElementById("content").innerHTML = `
        <div id="map" style="height: 70vh; width: 100%; border-radius: 15px;"></div>
        <button onclick="renderSearchResults()" style="margin-top:10px; width:100%;">⬅️ Назад к списку</button>
    `;

    const map = new maplibregl.Map({
        container: 'map',
        style: 'https://tiles.openfreemap.org/styles/positron',

        // Темная карта
        // style: 'https://tiles.openfreemap.org/styles/dark-matter',

        center: [userLongitude, userLatitude],
        zoom: 12
    });

    map.addControl(new maplibregl.NavigationControl());

searchResults.forEach(m => {
    const el = document.createElement('div');
    el.className = 'marker';
    el.innerHTML = '📍';
    el.style.fontSize = '24px';
    el.style.cursor = 'pointer';

    const hasOutgoing = outgoingInvites.some(inv => inv.receiverId === m.id);

    const hasIncoming = incomingInvites.some(inv => inv.senderId === m.id);

    const isLinked = hasOutgoing || hasIncoming;

    const instrumentsHtml = m.instruments ? m.instruments.map(ins =>
        `<span style="background:#eee; padding:2px 8px; border-radius:10px; font-size:10px; margin-right:4px; display:inline-block;">${ins.name}</span>`
    ).join('') : '';

    let actionHtml = '';
    if (isLinked) {
        const statusText = hasOutgoing ? "Запрос отправлен" : "Прислал вам запрос";
        actionHtml = `
            <button disabled
                    style="width: 100%; padding: 8px; background: #ccc; color: white; border: none; border-radius: 5px; cursor: default; font-weight: bold;">
                ⏳ ${statusText}
            </button>
        `;
    } else {
        actionHtml = `
            <button onclick="sendInvite('${m.id}','map')"
                    style="width: 100%; padding: 8px; background: #0088cc; color: white; border: none; border-radius: 5px; cursor: pointer; font-weight: bold;">
                📩 Отправить приглашение
            </button>
        `;
    }

    const popupContent = `
        <div style="font-family: sans-serif; width: 220px; padding: 5px;">
            <h3 style="margin: 0 0 5px 0;">${m.name}, ${m.age}</h3>
            <div style="margin-bottom: 8px;">${instrumentsHtml}</div>
            <p style="font-size: 13px; color: #555; margin: 0 0 10px 0; max-height: 60px; overflow-y: auto;">
                ${m.description || 'Нет описания'}
            </p>
            ${actionHtml}
            <div id="status-${m.id}" style="font-size: 11px; text-align: center; margin-top: 5px; color: green;"></div>
        </div>
    `;

    new maplibregl.Marker(el)
        .setLngLat([m.longitude, m.latitude])
        .setPopup(new maplibregl.Popup({ offset: 25, closeButton: true })
            .setHTML(popupContent))
        .addTo(map);
});
}



async function sendInvite(receiverId, window = "list"){

    const token = localStorage.getItem("jwt");

    try{
        const response = await fetch(`/profiles/invite/${receiverId}`,{
            method: 'POST',
            headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': `application/json`
            }
        });

        if(!response.ok) {
            const errorData = await response.json();
            alert("Ошибка: " + (errorData.message || "Не удалось отправить приглашение"));
        }

        await loadInvites();
        if(window === 'list') renderSearchResults();
        else if(window === 'map') renderMap();

    } catch (e) {}


}

async function renderInvitesScreen() {

    const content = document.getElementById('content');
    document.getElementById("title").innerText = "Приглашения";

    content.innerHTML = `
        <div class="invites-container" style="padding: 10px;">
            <div class="header-row" style="display: flex; align-items: center; margin-bottom: 15px; border-bottom: 1px solid #ccc;">
                <div class="tabs" style="display: flex; justify-content: space-around; margin-bottom: 15px; border-bottom: 1px solid #ccc;">
                    <button class="tab-btn ${currentTab === 'incoming' ? 'active-tab' : ''}"
                            onclick="switchTab('incoming')"
                            style="padding: 10px; border: none; background: none; cursor: pointer; ${currentTab === 'incoming' ? 'border-bottom: 2px solid #0088cc; color: #0088cc; font-weight: bold;' : ''}">
                        Входящие (${(incomingInvites || []).length})
                    </button>
                    <button class="tab-btn ${currentTab === 'outgoing' ? 'active-tab' : ''}"
                            onclick="switchTab('outgoing')"
                            style="padding: 10px; border: none; background: none; cursor: pointer; ${currentTab === 'outgoing' ? 'border-bottom: 2px solid #0088cc; color: #0088cc; font-weight: bold;' : ''}">
                        Исходящие (${(outgoingInvites || []).length})
                    </button>
                </div>
            </div>
            <div id="invites-list" class="list-container">
            </div>
                <button class="save-btn" style="width:100%; background: #666; margin-top: 10px; color: white; border: none; padding: 15px; border-radius: 10px;" onclick="renderMainScreen()">
                    ⬅️ Назад
                </button>
        </div>
    `;

    const invites = (currentTab === 'incoming') ? incomingInvites : outgoingInvites;

    renderList(invites);
}

window.switchTab = (tab) => {
    currentTab = tab;
    renderInvitesScreen();
};

async function fetchInvites(){

    const resp = await fetch(`"/profiles/invite"`,{
        headers: {'Authorization': `Bearer ${localStorage.getItem('jwt')}`
        }
    });

    return await response.json();
}

async function updateInviteStatus(inviteId, status){

     const resp = await fetch(`"/profiles/invite"`,{

            method: 'POST',
            headers: {'Authorization': `Bearer ${localStorage.getItem('jwt')}`
            }
        });

     renderInvitesScreen();

}

function renderList(invites) {
    const listContainer = document.getElementById('invites-list');

    if (!invites || invites.length === 0) {
        listContainer.innerHTML = '<p style="text-align:center; color: #888; margin-top: 20px;">Пока ничего нет</p>';
        return;
    }

    listContainer.innerHTML = invites.map(inv => {
        const isAccepted = inv.status === 'ACCEPTED';

        const displayName = (currentTab === 'incoming') ? `От: ${inv.senderScreenName || 'Музыкант'}` : `Кому: ${inv.receiverScreenName || 'Музыкант'}`;

        const age = (currentTab === 'incoming') ? inv.senderAge : inv.receiverAge;

        const instrumentsHtml = inv.instruments ? inv.instruments.map(ins =>
            `<span style="background:#eee; padding:2px 8px; border-radius:10px; font-size:10px; margin-right:4px; display:inline-block;">${ins.name}</span>`
        ).join('') : '';

        const desc =  (currentTab === 'incoming') ? inv.senderProfileDescription : inv.receiverProfileDescription;

        return `
            <div class="invite-card" style="background: white; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span style="font-weight: bold;">${displayName} ${age}</span>
                    <div style="margin-bottom: 8px;">${instrumentsHtml}</div>
                    <p style="font-size: 13px; color: #555; margin: 0 0 10px 0; max-height: 60px; overflow-y: auto;">
                        ${desc || 'Нет описания'}
                    </p>
                    <span style="font-size: 11px; color: #666; border: 1px solid #ddd; padding: 2px 5px; border-radius: 4px;">${inv.status}</span>
                </div>

                <div style="margin-top: 10px; display: flex; gap: 8px;">
                    ${renderInviteActions(inv)}
                </div>
            </div>
        `;
    }).join('');
}

function renderInviteActions(inv) {
    if (currentTab === 'outgoing') {
        let html = '';
        if (inv.status === 'ACCEPTED' /*&& inv.tgUsername*/) {
            html = `<a href="https://t.me/${inv.receiverTelegramUsername.replace('@','')}" target="_blank" style="background:#0088cc; color:white; text-decoration:none; padding:5px 10px; border-radius:5px; font-size:13px;">Написать</a>`;
        } else {
            html = `<button onclick="handleInviteAction('${inv.id}', 'delete')" style="background:#ff4d4d; color:white; border:none; padding:5px 10px; border-radius:5px;">Отменить</button>`;
        }
        return html;
    } else /* INCOMING */ {
        if (inv.status === 'PENDING') {
            return `
                <button onclick="handleInviteAction('${inv.id}', 'accept')" style="background:#4CAF50; color:white; border:none; padding:5px 10px; border-radius:5px;">Принять</button>
                <button onclick="handleInviteAction('${inv.id}', 'reject')" style="background:#888; color:white; border:none; padding:5px 10px; border-radius:5px;">Отклонить</button>
            `;
        } else if (inv.status === 'ACCEPTED') {
            return `<a href="https://t.me/${inv.senderTelegramUsername.replace('@','')}" target="_blank" style="background:#0088cc; color:white; text-decoration:none; padding:5px 10px; border-radius:5px; font-size:13px;">Написать</a>`;
        }
        return '';
    }
}

async function handleInviteAction(id, action){

    const jwt = localStorage.getItem("jwt");
    const url = `/profiles/invite/${id}`;
    let options = {
        headers: { "Authorization": "Bearer " + jwt }
    };

    switch(action){

        case "accept":
        case "reject":
            options.method = "PATCH";
            options.headers["Content-Type"] = "application/json";
            options.body = JSON.stringify({
            status: action === "accept" ? "ACCEPTED" : "REJECTED"
            });
        break;

        case "delete":
                    options.method = "DELETE";
        break;

    }


    try {
        const response = await fetch(url, options);

        if (response.ok) {
            await loadInvites();
            renderInvitesScreen();

            if (tg.HapticFeedback)
                tg.HapticFeedback.notificationOccurred('success');
            } else {
                console.error(`Ошибка при ${action}:`, response.status);
            }
        } catch (e) {
            console.error("Сетевая ошибка:", e);
        }

}

function subscribeToInviteEvents(){

    const jwt = localStorage.getItem("jwt");
    const eventSource = new EventSource(`/profiles/invite/subscribe?token=${jwt}`);

    eventSource.onmessage = (event) => {

        console.log("Пришло событие для обновления данных");
        loadInvites().then(() => {

            if (document.getElementById('invites-list')) {
                renderInvitesScreen();
            }

            if(document.getElementById('search-result')){
                renderSearchResults();
            }

            if(document.getElementById('map')){
                renderMap();
            }

        })
    };
}




