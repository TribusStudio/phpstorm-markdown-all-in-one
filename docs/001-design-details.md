# 001-design-details.md

**Author:** wilco
**Date:** 2026-03-08

## Introduction

This document will be used as a source of development ideas and guidelines as well
as a way to keep information institurionalized for future development.

## Ideas

- tables when in raw source in a Markdown file, if left to the formatting of the cells to the markdown engine and table formatter rules, could look something like this:

| Area           | Files                                                                                                                                                                                                                                                                                                |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| React Help     | js/app/src/components/help/HelpModal.jsx, SimpleHelpModal.jsx, helpTopics.js                                                                                                                                                                                                                         |
| React Tutorial | js/app/src/components/tutorial/TutorialOverlay.jsx, tutorialSteps.js                                                                                                                                                                                                                                 |
| React Context  | js/app/src/context/TutorialContext.jsx                                                                                                                                                                                                                                                               |
| Modified       | js/app/src/components/layout/PosShell.jsx (tutorial setup/teardown, touch resize), js/app/src/context/OrderContext.jsx (parkCurrentOrder, dispatch export)                                                                                                                                           |
| Refund picker  | OrderContext.jsx (refundMode state, 4 actions, getItemEffectiveTotal), CartPanel.jsx (refund bar, button, props), CartItem.jsx (refund/refunded visual states, inline refund amount), AdjustmentList.jsx (filter refund adjustments, recalled guard), OrderService.php (adjustment snapshot/restore) |

As can be seen in the source, the column delimiter as well as the raw text layout in the document is less than idea. In fact, its barely legible. The interesting thing about Markdown docs is that they naturally wrap their content when rules for rendering are multiline capable. We should have a smart table creation process which takes into consideration the PHPSTORM editor column count for text files. We can see the guideline for in in the editor. The table system should recognize this and build the table to fit as best as possible within that limitation. 

As well, the header row content should never have more than 1 space prepending and appending the value in the header cell. As well, the dividing line that follows the header row should always match the header width count. This would at least make the table header legible in any editor source window. The rendered version has no such restrictions since it flows according to HTML rules.

There should be a table setting that defines how the header row is rendered / updated in the editor. Whether to be minimum width possible or full width of the text column cound for the editor. I believe we currently have it set to 80 characters. In the settings for the markdown, we should be able to choose from the visual guides settings in PHPSTORM and decide which one the current markdown document will use for layout purposes. this could also simply be for the plugin settings.

The table above could look something like this instead:

| Area | Files |
| ---- | ----- |
| React Help     | js/app/src/components/help/HelpModal.jsx, SimpleHelpModal.jsx, helpTopics.js |
| React Tutorial | js/app/src/components/tutorial/TutorialOverlay.jsx, tutorialSteps.js |
| React Context  | js/app/src/context/TutorialContext.jsx |
| Modified       | js/app/src/components/layout/PosShell.jsx (tutorial setup/teardown, touch resize), js/app/src/context/OrderContext.jsx (parkCurrentOrder, dispatch export) |
| Refund picker  | OrderContext.jsx (refundMode state, 4 actions, getItemEffectiveTotal), CartPanel.jsx (refund bar, button, props), CartItem.jsx (refund/refunded visual states, inline refund amount), AdjustmentList.jsx (filter refund adjustments, recalled guard), OrderService.php (adjustment snapshot/restore) |

Here's another with 3 columns instead.

| Area | Files | Another Column |
| ---- | ----- | -------------- |
| React Help     | js/app/src/components/help/HelpModal.jsx, SimpleHelpModal.jsx, helpTopics.js | More content here |
| React Tutorial | js/app/src/components/tutorial/TutorialOverlay.jsx, tutorialSteps.js | Another line of content here which wraps ultimately on the next line as well |
| React Context  | js/app/src/context/TutorialContext.jsx | stuff |
| Modified       | js/app/src/components/layout/PosShell.jsx (tutorial setup/teardown, touch resize), js/app/src/context/OrderContext.jsx (parkCurrentOrder, dispatch export) | As does this line of content which wraps ultimately on the next line too. |
| Refund picker  | OrderContext.jsx (refundMode state, 4 actions, getItemEffectiveTotal), CartPanel.jsx (refund bar, button, props), CartItem.jsx (refund/refunded visual states, inline refund amount), AdjustmentList.jsx (filter refund adjustments, recalled guard), OrderService.php (adjustment snapshot/restore) | This is just the same thing as the other ones. |

Its far more compact. So, really, it should be a choice. Have the table content wrap to the visual guides OR fully auto-format it to full line width.

## Conclusions

- [Reference 1](url)
