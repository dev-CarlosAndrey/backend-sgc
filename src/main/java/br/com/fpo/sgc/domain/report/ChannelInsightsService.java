package br.com.fpo.sgc.domain.report;

import br.com.fpo.sgc.domain.report.ReportFilters.Period;
import br.com.fpo.sgc.domain.report.dto.ChannelReportDTO;
import br.com.fpo.sgc.domain.report.dto.NotificationReportDTO;

/** Relatórios sobre os canais: abertura de OS (painel × WhatsApp) e entrega das notificações. */
public interface ChannelInsightsService {
    ChannelReportDTO channels(Period filter);
    byte[] channelsCsv(Period filter);
    byte[] channelsPdf(Period filter);
    NotificationReportDTO notifications(Period filter);
    byte[] notificationsCsv(Period filter);
    byte[] notificationsPdf(Period filter);
}
