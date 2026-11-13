(function () {
  'use strict';

  // Mobile navigation toggle
  var toggle = document.querySelector('.nav-toggle');
  if (toggle) {
    toggle.addEventListener('click', function () {
      document.querySelector('.topnav').classList.toggle('open');
    });
  }

  // Confirm before delete (forms with data-confirm)
  document.querySelectorAll('form[data-confirm]').forEach(function (f) {
    f.addEventListener('submit', function (e) {
      if (!window.confirm(f.getAttribute('data-confirm'))) {
        e.preventDefault();
      }
    });
  });

  // Billing: total = consultation + medicine + lab + other (updates as you type)
  var bill = document.querySelector('form[data-module="bills"]');
  if (bill) {
    var names = ['consultation', 'medicine', 'lab', 'other'];
    var total = bill.querySelector('[name="total"]');
    var recalc = function () {
      var sum = 0;
      names.forEach(function (n) {
        var el = bill.querySelector('[name="' + n + '"]');
        sum += parseFloat(el && el.value) || 0;
      });
      total.value = sum.toFixed(2);
    };
    names.forEach(function (n) {
      var el = bill.querySelector('[name="' + n + '"]');
      if (el) { el.addEventListener('input', recalc); }
    });
    recalc();
  }

  // Appointments: do not allow booking in the past
  var apptDate = document.querySelector('form[data-module="appointments"] [name="appt_date"]');
  if (apptDate) {
    var d = new Date();
    var iso = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    if (!apptDate.value) { apptDate.min = iso; }
  }

  // Sign up: show doctor-only fields when role = doctor
  var roleSel = document.getElementById('roleSelect');
  if (roleSel) {
    var docBox = document.querySelector('.doctor-only');
    var toggleDoc = function () {
      var isDoc = roleSel.value === 'doctor';
      docBox.style.display = isDoc ? 'block' : 'none';
      docBox.querySelectorAll('input').forEach(function (i) { i.required = isDoc; });
    };
    roleSel.addEventListener('change', toggleDoc);
    toggleDoc();
  }

  // Auto-hide success messages
  var ok = document.querySelector('.alert.ok');
  if (ok) { setTimeout(function () { ok.style.display = 'none'; }, 5000); }
})();
