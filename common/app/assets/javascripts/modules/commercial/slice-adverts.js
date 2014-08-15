define([
    'bonzo',
    'qwery',
    'lodash/collections/contains',
    'lodash/functions/once',
    'lodash/objects/defaults',
    'common/utils/$',
    'common/utils/_',
    'common/utils/config',
    'common/utils/template',
    'common/modules/userPrefs',
    'common/modules/commercial/dfp'
], function (
    bonzo,
    qwery,
    contains,
    once,
    defaults,
    $,
    _,
    globalConfig,
    template,
    userPrefs,
    dfp
) {

    var adNames = ['inline1', 'inline2'],
        init = function(c) {

            var config = defaults(
                c || {},
                globalConfig,
                {
                    containerSelector: '.container',
                    sliceSelector: '.js-slice--ad-candidate',
                    page: {},
                    switches: {}
                }
            );

            if (!config.switches.standardAdverts) {
                return false;
            }

            // get all the containers
            var containers = qwery(config.containerSelector),
                index = 0,
                adSlices = [],
                containerGap = 2,
                prefs = userPrefs.get('container-states');
            // pull out ad slices which are have at least x containers between them
            while (index < containers.length) {
                var container = containers[index],
                    containerId = bonzo(container).data('id'),
                    $adSlice = $(config.sliceSelector, container),
                    // don't display ad in the first container on the fronts
                    isFrontFirst = contains(['uk', 'us', 'au'], config.page.pageId) && index === 0;
                if ($adSlice.length && !isFrontFirst && (!prefs || prefs[containerId] !== 'closed')) {
                    adSlices.push($adSlice.first());
                    index += (containerGap + 1);
                } else {
                    index++;
                }
            }

            _(adSlices)
                .slice(0, adNames.length)
                .forEach(function($adSlice, index) {
                    var adName = adNames[index],
                        $adSlot = bonzo(dfp.createAdSlot(adName, 'container-inline'));
                    $adSlice
                        .addClass('slice--has-ad')
                        .append($adSlot);
                })
                .valueOf();

            return adSlices;
        };

    return {

        init: once(init),

        // for testing
        reset: function() {
            this.init = once(init);
        }

    };

});
