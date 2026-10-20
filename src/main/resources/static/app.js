const API = "/api/v1";
const REFRESH_INTERVAL = 10000;

let refreshTimer = null;

$(document).ready(function () {
    $("#refresh").on("click", loadData);
    $("#station, #format, #warningLevel, #from, #to, #limit").on("change", loadData);
    $("#autoRefresh").on("change", updateAutoRefresh);

    loadStations();
    updateAutoRefresh();
});

function updateAutoRefresh() {
    clearInterval(refreshTimer);
    if ($("#autoRefresh").is(":checked")) {
        refreshTimer = setInterval(loadData, REFRESH_INTERVAL);
    }
}

function request(path, params) {
    const format = $("#format").val();
    return $.ajax({
        url: API + path,
        data: params,
        dataType: format,
        headers: { Accept: "application/" + format }
    });
}

function isXml() {
    return $("#format").val() === "xml";
}

// ---------- Laden ----------

function loadStations() {
    showLoading();
    request("/stations")
        .done(function (data) {
            const stations = isXml() ? listFromXml(data, stationFromXml) : data;
            const select = $("#station").empty();
            for (const station of stations) {
                const text = station.id + " - " + station.name + " (" + station.river + ")";
                select.append($("<option>").val(station.id).text(text));
            }
            loadData();
        })
        .fail(showError);
}

function loadData() {
    const stationId = $("#station").val();
    if (!stationId) {
        return;
    }
    showLoading();

    const latest = loadLatest(stationId);
    const history = loadHistory(stationId);
    const statistics = loadStatistics(stationId);
    const alerts = loadAlerts();

    $.when(latest, history, statistics, alerts).always(function () {
        $("#statusLine").text("Stand: " + new Date().toLocaleTimeString("de-AT")
            + " (Format: " + $("#format").val().toUpperCase() + ")");
    });
}

function loadLatest(stationId) {
    return request("/stations/" + stationId + "/measurements/latest")
        .done(function (data) {
            showLatest(isXml() ? measurementFromXml(data.documentElement) : data);
        })
        .fail(function (xhr) {
            $("#latest").html($("<p class='empty'>").text(errorMessage(xhr)));
            if (xhr.status !== 404) {
                showError(xhr);
            }
        });
}

function loadHistory(stationId) {
    const params = { limit: $("#limit").val() };
    if ($("#from").val()) {
        params.from = new Date($("#from").val()).toISOString();
    }
    if ($("#to").val()) {
        params.to = new Date($("#to").val()).toISOString();
    }
    if ($("#warningLevel").val()) {
        params.warningLevel = $("#warningLevel").val();
    }

    return request("/stations/" + stationId + "/measurements", params)
        .done(function (data) {
            showHistory(isXml() ? listFromXml(data, measurementFromXml) : data);
        })
        .fail(showError);
}

function loadStatistics(stationId) {
    return request("/stations/" + stationId + "/statistics")
        .done(function (data) {
            showStatistics(isXml() ? statisticsFromXml(data.documentElement) : data);
        })
        .fail(showError);
}

function loadAlerts() {
    return request("/alerts")
        .done(function (data) {
            showAlerts(isXml() ? listFromXml(data, alertFromXml) : data);
        })
        .fail(showError);
}

// ---------- XML auswerten ----------

function listFromXml(xml, convert) {
    const list = [];
    $(xml).find("item").each(function () {
        list.push(convert(this));
    });
    return list;
}

function text(node, name) {
    return $(node).children(name).text();
}

function number(node, name) {
    return parseFloat(text(node, name));
}

function stationFromXml(node) {
    return {
        id: text(node, "id"),
        name: text(node, "name"),
        river: text(node, "river")
    };
}

function measurementFromXml(node) {
    return {
        stationId: text(node, "stationId"),
        timestamp: text(node, "timestamp"),
        waterLevel: number(node, "waterLevel"),
        flowRate: number(node, "flowRate"),
        rainfall: number(node, "rainfall"),
        temperature: number(node, "temperature"),
        batteryLevel: number(node, "batteryLevel"),
        status: text(node, "status"),
        warningLevel: text(node, "warningLevel")
    };
}

function alertFromXml(node) {
    return {
        stationId: text(node, "stationId"),
        stationName: text(node, "stationName"),
        river: text(node, "river"),
        timestamp: text(node, "timestamp"),
        waterLevel: number(node, "waterLevel"),
        warningLevel: text(node, "warningLevel")
    };
}

function statisticsFromXml(node) {
    return {
        measurementCount: number(node, "measurementCount"),
        minWaterLevel: number(node, "minWaterLevel"),
        maxWaterLevel: number(node, "maxWaterLevel"),
        averageWaterLevel: number(node, "averageWaterLevel"),
        averageFlowRate: number(node, "averageFlowRate"),
        rainfallSum: number(node, "rainfallSum"),
        warningCount: number(node, "warningCount"),
        criticalCount: number(node, "criticalCount")
    };
}

// ---------- Anzeigen ----------

function showLatest(measurement) {
    $("#latest").empty()
        .append(card(measurement.waterLevel + " cm", "Wasserstand"))
        .append(card(measurement.flowRate + " m³/s", "Durchfluss"))
        .append(card(measurement.rainfall + " mm/h", "Niederschlag"))
        .append(card(measurement.temperature + " °C", "Temperatur"))
        .append(card(measurement.batteryLevel + " %", "Akku"))
        .append(card(measurement.status, "Status"))
        .append(card(badge(measurement.warningLevel), "Warnstufe"))
        .append(card(formatTime(measurement.timestamp), "Zeitpunkt"));
}

function showStatistics(statistics) {
    $("#statistics").empty()
        .append(card(statistics.measurementCount, "Messungen"))
        .append(card(statistics.minWaterLevel + " cm", "Niedrigster Wasserstand"))
        .append(card(statistics.maxWaterLevel + " cm", "Höchster Wasserstand"))
        .append(card(statistics.averageWaterLevel + " cm", "Ø Wasserstand"))
        .append(card(statistics.averageFlowRate + " m³/s", "Ø Durchfluss"))
        .append(card(statistics.rainfallSum + " mm", "Summe Niederschlag"))
        .append(card(statistics.warningCount, "Warnungen"))
        .append(card(statistics.criticalCount, "Kritische Warnungen"));
}

function showAlerts(alerts) {
    const list = $("#alerts").empty();
    if (alerts.length === 0) {
        list.append($("<li class='empty'>").text("Keine Warnungen"));
        return;
    }
    for (const alert of alerts) {
        const description = " " + alert.stationId + " - " + alert.stationName + " (" + alert.river + "): "
            + alert.waterLevel + " cm, " + formatTime(alert.timestamp);
        list.append($("<li>").append(badge(alert.warningLevel)).append(document.createTextNode(description)));
    }
}

function showHistory(measurements) {
    const body = $("#history").empty();
    if (measurements.length === 0) {
        body.append($("<tr>").append($("<td colspan='8' class='empty'>").text("Keine Messwerte gefunden")));
        return;
    }
    for (const measurement of measurements) {
        const row = $("<tr>");
        row.append($("<td>").text(measurement.stationId));
        row.append($("<td>").text(formatTime(measurement.timestamp)));
        row.append($("<td>").text(measurement.waterLevel + " cm"));
        row.append($("<td>").text(measurement.flowRate + " m³/s"));
        row.append($("<td>").text(measurement.rainfall + " mm/h"));
        row.append($("<td>").text(measurement.batteryLevel + " %"));
        row.append($("<td>").text(measurement.status));
        row.append($("<td>").append(badge(measurement.warningLevel)));
        body.append(row);
    }
}

function card(value, label) {
    const valueElement = $("<div class='value'>");
    if (typeof value === "object") {
        valueElement.append(value);
    } else {
        valueElement.text(value);
    }
    return $("<div class='card'>").append(valueElement).append($("<div class='label'>").text(label));
}

function badge(warningLevel) {
    return $("<span class='badge'>").addClass(warningLevel).text(warningLevel);
}

function formatTime(timestamp) {
    return new Date(timestamp).toLocaleString("de-AT");
}

// ---------- Status und Fehler ----------

function showLoading() {
    $("#errorLine").hide();
    $("#statusLine").text("Lade Daten ...");
}

function showError(xhr) {
    $("#errorLine").text("Fehler: " + errorMessage(xhr)).show();
}

function errorMessage(xhr) {
    if (xhr.status === 0) {
        return "Der Server ist nicht erreichbar";
    }
    const contentType = xhr.getResponseHeader("Content-Type") || "";
    try {
        if (contentType.includes("xml")) {
            return $($.parseXML(xhr.responseText)).find("message").text();
        }
        return JSON.parse(xhr.responseText).message;
    } catch (error) {
        return "Unerwartete Antwort vom Server (Status " + xhr.status + ")";
    }
}
