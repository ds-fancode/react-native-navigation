import cloneDeepWith from 'lodash/cloneDeepWith';
import cloneDeep from 'lodash/cloneDeep';
import map from 'lodash/map';
import { CommandsObserver } from '../events/CommandsObserver';
import { UniqueIdProvider } from '../adapters/UniqueIdProvider';
import { Options } from '../interfaces/Options';
import { Layout, LayoutRoot } from '../interfaces/Layout';
import { LayoutTreeParser } from './LayoutTreeParser';
import { LayoutTreeCrawler } from './LayoutTreeCrawler';
import { OptionsProcessor } from './OptionsProcessor';
import { Store } from '../components/Store';
import { LayoutProcessor } from '../processors/LayoutProcessor';
import { CommandName } from '../interfaces/CommandName';
import { OptionsCrawler } from './OptionsCrawler';

export class CommandsCreator {
  constructor(
    private readonly store: Store,
    private readonly layoutTreeParser: LayoutTreeParser,
    private readonly layoutTreeCrawler: LayoutTreeCrawler,
    private readonly commandsObserver: CommandsObserver,
    private readonly uniqueIdProvider: UniqueIdProvider,
    private readonly optionsProcessor: OptionsProcessor,
    private readonly layoutProcessor: LayoutProcessor,
    private readonly optionsCrawler: OptionsCrawler
  ) {}

  public setRoot(simpleApi: LayoutRoot) {
    const input = cloneLayout(simpleApi);
    this.optionsCrawler.crawl(input.root);
    const processedRoot = this.layoutProcessor.process(input.root, CommandName.SetRoot);
    const root = this.layoutTreeParser.parse(processedRoot);

    const modals = map(input.modals, (modal) => {
      this.optionsCrawler.crawl(modal);
      const processedModal = this.layoutProcessor.process(modal, CommandName.SetRoot);
      return this.layoutTreeParser.parse(processedModal);
    });

    const overlays = map(input.overlays, (overlay: any) => {
      this.optionsCrawler.crawl(overlay);
      const processedOverlay = this.layoutProcessor.process(overlay, CommandName.SetRoot);
      return this.layoutTreeParser.parse(processedOverlay);
    });

    const commandId = this.uniqueIdProvider.generate(CommandName.SetRoot);

    this.layoutTreeCrawler.crawl(root, CommandName.SetRoot);
    modals.forEach((modalLayout) => {
      this.layoutTreeCrawler.crawl(modalLayout, CommandName.SetRoot);
    });
    overlays.forEach((overlayLayout) => {
      this.layoutTreeCrawler.crawl(overlayLayout, CommandName.SetRoot);
    });

    return Promise.resolve({
      args: [commandId, { root, modals, overlays }],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.SetRoot, {
          commandId: newCommandId ?? commandId,
          layout: { root, modals, overlays },
        });
      },
    });
  }

  public setDefaultOptions(options: Options) {
    const input = cloneDeep(options);
    this.optionsProcessor.processDefaultOptions(input, CommandName.SetDefaultOptions);
    return Promise.resolve({
      args: [input],
      notify: () => {
        this.commandsObserver.notify(CommandName.SetDefaultOptions, { options });
      },
    });
  }

  public mergeOptions(componentId: string, options: Options) {
    const input = cloneDeep(options);
    this.optionsProcessor.processOptions(input, CommandName.MergeOptions);

    const component = this.store.getComponentInstance(componentId);
    if (component && !component.isMounted)
      console.warn(
        `Navigation.mergeOptions was invoked on component with id: ${componentId} before it is mounted, this can cause UI issues and should be avoided.\n Use static options instead.`
      );

    return Promise.resolve({
      args: [componentId, input],
      notify: () => {
        this.commandsObserver.notify(CommandName.MergeOptions, { componentId, options });
      },
    });
  }

  public showModal(layout: Layout) {
    const layoutCloned = cloneLayout(layout);
    this.optionsCrawler.crawl(layoutCloned);
    const layoutProcessed = this.layoutProcessor.process(layoutCloned, CommandName.ShowModal);
    const layoutNode = this.layoutTreeParser.parse(layoutProcessed);

    const commandId = this.uniqueIdProvider.generate(CommandName.ShowModal);
    this.layoutTreeCrawler.crawl(layoutNode, CommandName.ShowModal);

    return Promise.resolve({
      args: [commandId, layoutNode],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.ShowModal, {
          commandId: newCommandId ?? commandId,
          layout: layoutNode,
        });
      },
    });
  }

  public dismissModal(componentId: string, mergeOptions?: Options) {
    const commandId = this.uniqueIdProvider.generate(CommandName.DismissModal);
    return Promise.resolve({
      args: [commandId, componentId, mergeOptions],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.DismissModal, {
          commandId: newCommandId ?? commandId,
          componentId,
          mergeOptions,
        });
      },
    });
  }

  public dismissAllModals(mergeOptions?: Options) {
    const commandId = this.uniqueIdProvider.generate(CommandName.DismissAllModals);
    return Promise.resolve({
      args: [commandId, mergeOptions],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.DismissAllModals, {
          commandId: newCommandId ?? commandId,
          mergeOptions,
        });
      },
    });
  }

  public push(componentId: string, simpleApi: Layout) {
    const input = cloneLayout(simpleApi);
    this.optionsCrawler.crawl(input);
    const layoutProcessed = this.layoutProcessor.process(input, CommandName.Push);
    const layout = this.layoutTreeParser.parse(layoutProcessed);

    const commandId = this.uniqueIdProvider.generate(CommandName.Push);
    this.layoutTreeCrawler.crawl(layout, CommandName.Push);

    return Promise.resolve({
      args: [commandId, componentId, layout],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.Push, {
          commandId: newCommandId ?? commandId,
          componentId,
          layout,
        });
      },
    });
  }

  public pop(componentId: string, mergeOptions?: Options) {
    const commandId = this.uniqueIdProvider.generate(CommandName.Pop);
    return Promise.resolve({
      args: [commandId, componentId, mergeOptions],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.Pop, {
          commandId: newCommandId ?? commandId,
          componentId,
          mergeOptions,
        });
      },
    });
  }

  public popTo(componentId: string, mergeOptions?: Options) {
    const commandId = this.uniqueIdProvider.generate(CommandName.PopTo);
    return Promise.resolve({
      args: [commandId, componentId, mergeOptions],
      nofity: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.PopTo, {
          commandId: newCommandId ?? commandId,
          componentId,
          mergeOptions,
        });
      },
    });
  }

  public popToRoot(componentId: string, mergeOptions?: Options) {
    const commandId = this.uniqueIdProvider.generate(CommandName.PopToRoot);
    return Promise.resolve({
      args: [commandId, componentId, mergeOptions],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.PopToRoot, {
          commandId: newCommandId ?? commandId,
          componentId,
          mergeOptions,
        });
      },
    });
  }

  public setStackRoot(componentId: string, children: Layout[]) {
    const input = map(cloneLayout(children), (simpleApi) => {
      this.optionsCrawler.crawl(simpleApi);
      const layoutProcessed = this.layoutProcessor.process(simpleApi, CommandName.SetStackRoot);
      const layout = this.layoutTreeParser.parse(layoutProcessed);
      return layout;
    });

    const commandId = this.uniqueIdProvider.generate(CommandName.SetStackRoot);
    input.forEach((layoutNode) => {
      this.layoutTreeCrawler.crawl(layoutNode, CommandName.SetStackRoot);
    });

    return Promise.resolve({
      args: [commandId, componentId, input],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.SetStackRoot, {
          commandId: newCommandId ?? commandId,
          componentId,
          layout: input,
        });
      },
    });
  }

  public showOverlay(simpleApi: Layout) {
    const input = cloneLayout(simpleApi);
    this.optionsCrawler.crawl(input);
    const layoutProcessed = this.layoutProcessor.process(input, CommandName.ShowOverlay);
    const layout = this.layoutTreeParser.parse(layoutProcessed);

    const commandId = this.uniqueIdProvider.generate(CommandName.ShowOverlay);
    this.layoutTreeCrawler.crawl(layout, CommandName.ShowOverlay);

    return Promise.resolve({
      args: [commandId, layout],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.ShowOverlay, {
          commandId: newCommandId ?? commandId,
          layout,
        });
      },
    });
  }

  public dismissOverlay(componentId: string) {
    const commandId = this.uniqueIdProvider.generate(CommandName.DismissOverlay);
    return Promise.resolve({
      args: [commandId, componentId],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.DismissOverlay, {
          commandId: newCommandId ?? commandId,
          componentId,
        });
      },
    });
  }

  public dismissAllOverlays() {
    const commandId = this.uniqueIdProvider.generate(CommandName.DismissAllOverlays);
    return Promise.resolve({
      args: [commandId],
      notify: (newCommandId?: string) => {
        this.commandsObserver.notify(CommandName.DismissAllOverlays, {
          commandId: newCommandId ?? commandId,
        });
      },
    });
  }
}

function cloneLayout<L>(layout: L): L {
  return cloneDeepWith(layout, (value, key) => {
    if (key === 'passProps' && typeof value === 'object' && value !== null) return { ...value };
  });
}
