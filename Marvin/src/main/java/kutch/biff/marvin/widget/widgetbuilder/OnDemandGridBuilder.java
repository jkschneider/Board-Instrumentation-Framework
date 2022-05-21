/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kutch.biff.marvin.widget.widgetbuilder;

import java.util.logging.Logger;

import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.task.TaskManager;
import kutch.biff.marvin.utility.AliasMgr;
import kutch.biff.marvin.widget.GridWidget;
import kutch.biff.marvin.widget.OnDemandGridWidget;
import kutch.biff.marvin.widget.Widget;

/**
 * @author Patrick.Kutch@gmail.com
 */
public class OnDemandGridBuilder implements OnDemandWidgetBuilder {
    private static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());
    private OnDemandGridWidget containerGrid;
    private int builtCount;

    public OnDemandGridBuilder(OnDemandGridWidget objParent) {
        containerGrid = objParent;
    }

    @Override
    public boolean Build(String namespace, String id, String value, String sortStr) {
        LOGGER.info("Creating OnDemand Grid for namespace: " + namespace + " and ID: " + id);
        builtCount += 1;
        AliasMgr.getAliasMgr().PushAliasList(true);
        containerGrid.getCriterea().putAliasListSnapshot();
        // __containerGrid.AddAliasListSnapshot();
        AliasMgr.getAliasMgr().PushAliasList(true);
        AliasMgr.getAliasMgr().AddAlias("TriggeredNamespace", namespace); // So tab knows namespace
        AliasMgr.getAliasMgr().AddAlias("TriggeredID", id);
        AliasMgr.getAliasMgr().AddAlias("TriggeredValue", value);
        AliasMgr.getAliasMgr().AddAlias("TriggeredIndex", Integer.toString(builtCount));
        containerGrid.getCriterea().tokenizeAndCreateAlias(id);
        // Let's throw in if it is odd or even :-)
        if (builtCount % 2 == 0) {
            AliasMgr.getAliasMgr().AddAlias("TriggeredEVEN", "TRUE");
        } else {
            AliasMgr.getAliasMgr().AddAlias("TriggeredEVEN", "FALSE");
        }

        Widget objWidget = WidgetBuilder.Build(containerGrid.getCriterea().getNode());
        if (null == objWidget) {
            return false;
        }
        if (!(objWidget instanceof GridWidget)) {
            LOGGER.severe("Tried to build something that was not a Grid " + objWidget.getClass().toString());
            return false;
        }
        // once for this grid's aliases and another for the 'super set' stored
        AliasMgr.getAliasMgr().PopAliasList();
        AliasMgr.getAliasMgr().PopAliasList();
        GridWidget objGridWidget = (GridWidget) objWidget;
        if (null != objGridWidget.getOnDemandTask()) {
            TaskManager.getTaskManager().AddDeferredTask(objGridWidget.getOnDemandTask());
        }
        return containerGrid.AddOnDemandWidget(objGridWidget, sortStr);
    }

}
