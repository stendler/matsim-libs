package lsp.resourceImplementations.mainRunCarrier;

import lsp.LSPResource;
import lsp.LogisticChainElement;
import lsp.resourceImplementations.UsecaseUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;

import java.util.ArrayList;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class MainRunCarrierUtils {
    public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler() {
        return new MainRunCarrierScheduler();
    }

    public static class MainRunCarrierResourceBuilder {

        private final Id<LSPResource> id;
        private final ArrayList<LogisticChainElement> clientElements;
        private final Network network;
        private Carrier carrier;
        private Id<Link> fromLinkId;
        private Id<Link> toLinkId;
        private MainRunCarrierScheduler mainRunScheduler;
        private UsecaseUtils.VehicleReturn vehicleReturn;

        private MainRunCarrierResourceBuilder(Carrier carrier, Network network) {
            this.id = Id.create(carrier.getId().toString(), LSPResource.class);
            UsecaseUtils.setCarrierType(carrier, UsecaseUtils.CARRIER_TYPE.mainRunCarrier);
            this.carrier = carrier;
            this.clientElements = new ArrayList<>();
            this.network = network;
        }

        public static MainRunCarrierResourceBuilder newInstance(Carrier carrier, Network network) {
            return new MainRunCarrierResourceBuilder(carrier, network);
        }

        public MainRunCarrierResourceBuilder setCarrier(Carrier carrier) {
            UsecaseUtils.setCarrierType(carrier, UsecaseUtils.CARRIER_TYPE.mainRunCarrier);
            this.carrier = carrier;
            return this;
        }

        public MainRunCarrierResourceBuilder setFromLinkId(Id<Link> fromLinkId) {
            this.fromLinkId = fromLinkId;
            return this;
        }

        public MainRunCarrierResourceBuilder setToLinkId(Id<Link> toLinkId) {
            this.toLinkId = toLinkId;
            return this;
        }

        public MainRunCarrierResourceBuilder setMainRunCarrierScheduler(MainRunCarrierScheduler mainRunScheduler) {
            this.mainRunScheduler = mainRunScheduler;
            return this;
        }

        public MainRunCarrierResourceBuilder setVehicleReturn(UsecaseUtils.VehicleReturn vehicleReturn) {
            this.vehicleReturn = vehicleReturn;
            return this;
        }

        public MainRunCarrierResource build() {
            return new MainRunCarrierResource(this);
        }

        //--- Getter ---

        Id<LSPResource> getId() {
            return id;
        }

        Carrier getCarrier() {
            return carrier;
        }

        Id<Link> getFromLinkId() {
            return fromLinkId;
        }

        Id<Link> getToLinkId() {
            return toLinkId;
        }

        ArrayList<LogisticChainElement> getClientElements() {
            return clientElements;
        }

        MainRunCarrierScheduler getMainRunScheduler() {
            return mainRunScheduler;
        }

        Network getNetwork() {
            return network;
        }

        UsecaseUtils.VehicleReturn getVehicleReturn() {
            return vehicleReturn;
        }
    }
}
