// recommendations.test.js

const mockMap = {
    setCenter: jest.fn(),
    setZoom: jest.fn(),
};
const mockGeocoder = {
    geocode: jest.fn(), 
};
const mockMarker = jest.fn();

const mockWriteText = jest.fn(() => Promise.resolve());

jest.useFakeTimers();

function initMap(mapElementId, locationString) {
    if (typeof google === 'undefined' || typeof google.maps === 'undefined') {
        console.error('Google Maps API not loaded.'); 
        return;
    }
    
    const geocoder = new google.maps.Geocoder();
    const mapDiv = document.getElementById(mapElementId);
    
    if (!mapDiv) return; 

    geocoder.geocode({ 'address': locationString }, (results, status) => {
        if (status === 'OK' && results[0]) {
            const mapOptions = {
                center: results[0].geometry.location,
                zoom: 15,
                mapTypeId: google.maps.MapTypeId.ROADMAP,
                fullscreenControl: false,
                streetViewControl: false,
                mapTypeControl: false,
                zoomControl: true,
            };
            
            const map = new google.maps.Map(mapDiv, mapOptions);

            new google.maps.Marker({
                map: map,
                position: results[0].geometry.location,
                title: locationString
            });
        } 
        else {
            console.error('Geocode was not successful for ' + locationString + ' for the following reason: ' + status);
            mapDiv.innerHTML = '<p style="text-align:center;">Location map unavailable (' + status + ').</p>';
        }
    });
}

function loadMaps() {
    const eventCards = document.querySelectorAll('.event-card');
    
    eventCards.forEach(card => {
        const mapElement = card.querySelector('.event-map');
        const locationSpan = card.querySelector('.event-location');
        
        if (mapElement && locationSpan) { 
            const mapId = mapElement.id;
            const location = locationSpan.textContent.trim();
            
            if (location && mapId) { 
                initMap(mapId, location);
            }
        }
    });
}

function copyShareLink(buttonElement) {
    const shareUrl = buttonElement.getAttribute('data-event-url');
    const fullUrl = window.location.origin + shareUrl; 

    if (navigator.clipboard && navigator.clipboard.writeText) { 
        navigator.clipboard.writeText(fullUrl)
            .then(() => {
                const originalText = buttonElement.innerHTML;
                buttonElement.innerHTML = "Copied!";
                
                setTimeout(() => {
                    buttonElement.innerHTML = originalText;
                }, 2000);
            })
            .catch(err => { 
                console.error('Failed to copy text: ', err);
                console.warn('Could not automatically copy the link. Please copy this URL manually: ' + fullUrl);
            });
    } 
    else { 
        console.warn('Please copy this URL manually: ' + fullUrl);
    }
}

describe('Google Maps Integration Functions', () => {
    const mockCoordinates = { lat: -37.8136, lng: 144.9631 };
    let consoleErrorSpy, consoleWarnSpy;

    beforeEach(() => {
        global.google = {
            maps: {
                Map: jest.fn(() => mockMap),
                Geocoder: jest.fn(() => mockGeocoder),
                Marker: mockMarker,
                MapTypeId: { ROADMAP: 'roadmap' }
            }
        };
        global.navigator = {
            clipboard: { writeText: mockWriteText }
        };

        document.body.innerHTML = `
            <article class="event-card">
                <span id="location-101" class="event-location">RMIT University</span>
                <div id="map-101" class="event-map"><p>Loading Map...</p></div>
            </article>
            <article class="event-card" id="incomplete-card">
                <span id="location-103" class="event-location">Valid Location</span>
            </article>
        `;
        
        mockGeocoder.geocode.mockClear();
        google.maps.Map.mockClear();
        mockMarker.mockClear();
        consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    });
    
    afterEach(() => {
        consoleErrorSpy.mockRestore();
        consoleWarnSpy.mockRestore();
    });

    describe('initMap', () => {
        test('BRANCH 1: Handles Google Maps API not being loaded', () => {
            global.google.maps = undefined;
            initMap('map-101', 'RMIT University');
            expect(consoleErrorSpy).toHaveBeenCalledWith('Google Maps API not loaded.');
            expect(mockGeocoder.geocode).not.toHaveBeenCalled(); 
        });

        test('BRANCH 2: Returns early if map element ID does not exist', () => {
            initMap('non-existent-map-id', 'Location');
            expect(mockGeocoder.geocode).not.toHaveBeenCalled();
        });
        
        test('BRANCH 3: Initializes map on successful geocode', () => {
            mockGeocoder.geocode.mockImplementationOnce((params, callback) => {
                callback([{ geometry: { location: mockCoordinates } }], 'OK');
            });
            initMap('map-101', 'RMIT University');
            expect(google.maps.Map).toHaveBeenCalledTimes(1);
            expect(mockMarker).toHaveBeenCalledTimes(1);
        });

        test('BRANCH 4: Displays error message on failed geocode', () => {
            mockGeocoder.geocode.mockImplementationOnce((params, callback) => {
                callback([], 'ZERO_RESULTS');
            });
            const mapDiv = document.getElementById('map-101');
            initMap('map-101', 'Non-existent Location');
            expect(google.maps.Map).not.toHaveBeenCalled();
            expect(mapDiv.innerHTML).toContain('Location map unavailable (ZERO_RESULTS)');
        });
    });

    describe('loadMaps', () => {
        test('BRANCH 5: Skips card if map or location element is missing', () => {
            mockGeocoder.geocode.mockImplementation(() => {}); 
            loadMaps();
            expect(mockGeocoder.geocode).toHaveBeenCalledTimes(1); 
            expect(mockGeocoder.geocode).toHaveBeenCalledWith(
                { 'address': 'RMIT University' },
                expect.any(Function)
            );
        });
        
        test('BRANCH 6: Skips initMap if location string is empty', () => {
            document.body.innerHTML = `
                <article class="event-card">
                    <span id="location-104" class="event-location"> </span>
                    <div id="map-104" class="event-map"></div>
                </article>
            `;
            mockGeocoder.geocode.mockImplementation(() => {});
            loadMaps();
            expect(mockGeocoder.geocode).not.toHaveBeenCalled(); 
        });
    });
});

describe('copyShareLink', () => {
    let buttonElement;
    let consoleErrorSpy, consoleWarnSpy;

    beforeEach(() => {
        document.body.innerHTML = `<button id="shareBtn" data-event-url="/events/101">Share 🔗</button>`;
        buttonElement = document.getElementById('shareBtn');
        
        window.location.origin = 'http://localhost'; 

        mockWriteText.mockClear();
        buttonElement.innerHTML = 'Share 🔗';
        
        consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
        
        global.navigator.clipboard = { writeText: mockWriteText };
    });
    
    afterEach(() => {
        consoleErrorSpy.mockRestore();
        consoleWarnSpy.mockRestore();
    });

    test('BRANCH 7: Executes .then() on successful copy and reverts text', async () => {
        const expectedUrl = 'http://localhost/events/101'; 
        copyShareLink(buttonElement);
        expect(mockWriteText).toHaveBeenCalledWith(expectedUrl);
        await mockWriteText.mock.results[0].value; 
        expect(buttonElement.innerHTML).toBe('Copied!');
        jest.advanceTimersByTime(2000); 
        expect(buttonElement.innerHTML).toBe('Share 🔗');
    });

    test('BRANCH 9: Executes else branch if clipboard API is unavailable', () => {
        global.navigator.clipboard = undefined; 
        const expectedUrl = 'http://localhost/events/101'; 
        
        copyShareLink(buttonElement);
        
        expect(mockWriteText).not.toHaveBeenCalled();
        expect(consoleWarnSpy).toHaveBeenCalledWith('Please copy this URL manually: ' + expectedUrl);
    });
});