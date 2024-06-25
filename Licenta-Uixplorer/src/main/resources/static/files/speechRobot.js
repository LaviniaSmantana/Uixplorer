
var isSpeaking = false;
var paused = false;
var speech;

document.getElementById('readButton').addEventListener('click', function() {
        var icon = document.getElementById('playButton');
        if (icon.classList.contains('fa-circle-play') && !paused) {
            icon.classList.remove("fa-circle-play");
            icon.classList.add("fa-circle-pause");
            startSpeaking();
        } else if(icon.classList.contains('fa-circle-play') && paused) {
            icon.classList.remove("fa-circle-play");
            icon.classList.add("fa-circle-pause");
            resumeSpeaking();
        } else if(icon.classList.contains('fa-circle-pause')){
            icon.classList.remove("fa-circle-pause");
            icon.classList.add("fa-circle-play");
            stopSpeaking();
        }
});

function startSpeaking() {

    <!-- Anulam orice sinteza vocala existenta inainte de a incepe o noua sinteza -->
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
        }

        var mainContent = document.getElementById('articleBody');
        var textToRead = mainContent.innerText;

        if ('speechSynthesis' in window) {
            speech = new SpeechSynthesisUtterance(textToRead);
            speech.lang = 'en-EN';
            speech.volume = 1;
            speech.rate = 1.3;
            speech.pitch = 1.8;

            window.speechSynthesis.speak(speech);

            isSpeaking = true;
            paused = false; // Reset paused state
        } else {
            alert('Browser-ul nu suporta functia de vorbire text!');
        }
}

function stopSpeaking() {
        if (isSpeaking) {
            window.speechSynthesis.pause();
            isSpeaking = false;
            paused = true;
        }
}

function resumeSpeaking(){
        if (!isSpeaking && paused){
            window.speechSynthesis.resume();
            isSpeaking = true;
            paused = false;
        }
}

<!-- Anulam orice sinteza vocala existenta inainte de a parasi pagina -->

window.addEventListener('beforeunload', function() {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
    }
});